package net.ofk.konfiguration

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import net.ofk.kutils.Auto
import net.ofk.kutils.BaseInvocationHandler
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.util.Properties

/**
 * The factory can create proxies for configuration interfaces.
 * Configuration interfaces define methods which when called return values
 * found in the configuration files or in the system properties.
 * The files and the system properties should be written in the HOCON format.
 * The factory looks for properties in jarKonfiguration.props files
 * which should be on the classpath.
 * Every library may provide its own configuration file,
 * this is basically the library default configuration.
 * To avoid name clashes it's recommended to use dot-like namespaces,
 * for example, net.ofk.kplatform.initType.
 * Application wide configuration may be either on the classpath or on the file system.
 * It's properties have higher priority and override library defaults.
 * Deployment manager can provide their own application wide configuration.
 * Application developers should define a special system property name,
 * the path to the system wide configuration file is defined by this special property.
 * So, which application wide configuration file
 * (the one provided by the application developers or by the deployment manager)
 * will be used is defined by this property. But they cannot be used at the same time,
 * one of them have to be selected and this is done exactly by means of that special
 * system property which name (as mentioned) is defined only by application developers.
 * If the deployment manager wishes to provide their own configuration
 * they can take the one provided by the developers, copy and adjust it accordingly.
 * It's recommended to specify the special property value outside the application,
 * for example, as a parameter passed to the application executable
 * (binary or startup script, for instance).
 * If the property starts with classpath: prefix
 * the application wide configuration will be searched on the classpath.
 * Users also can override library and application defaults by setting property values
 * and passing them as system properties. The system properties have the highest priority.
 */
class KonfigurationFactory {
  companion object {
    private val JAR_CONFIGURATION_PATH = "jarKonfiguration.props"
    private val CLASSPATH_PREFIX = "classpath:"
    private val PROPERTIES_METHOD_NAME = "properties"
  }

  /**
   * Creates a proxy for an interface.
   * Methods of the proxy implementing the specified interface
   * will return values taken from configuration files or from system properties.
   * The configuration files that are looked through for the values
   * are all the resources on the classpath with path jarKonfiguration.props;
   * a resource on the classpath or on the file system as specified by a system property
   * which name is passed as @param configFilePathPropName;
   * system properties have the highest priority.
   */
  fun <T> load(type: Class<T>, configFilePathPropName: String? = null): T {
    var config = ConfigFactory.parseProperties(Properties())
    type.classLoader.getResources(JAR_CONFIGURATION_PATH).toList().forEach {r ->
      config = ConfigFactory.parseURL(r.toURI().toURL()).withFallback(config)
    }

    if (configFilePathPropName != null) {
      val url = System.getProperty(configFilePathPropName)
      Auto.close {
        val reader = InputStreamReader((if (url.startsWith(CLASSPATH_PREFIX)) {
          type.classLoader.getResourceAsStream(url.toString().substring(CLASSPATH_PREFIX.length))
        } else {
          FileInputStream(url)
        })).open()

        config = ConfigFactory.parseReader(reader).withFallback(config)
      }
    }

    return Proxy.newProxyInstance(type.classLoader, arrayOf(type), Handler(type, "", listOf(), config)) as T
  }

  private class Handler(type: Class<*>, private val prefix: String, private val parents: List<String>, private var config: Config) : BaseInvocationHandler() {
    private val map = hashMapOf<Method, Lazy<Any?>>()

    init {
      this.validateType(if (prefix.isEmpty()) prefix else prefix.substring(prefix.length - 1), type)
    }

    override fun doInvoke(proxy: Any, method: Method, args: Array<Any?>?): Any? =
      if (method.name == PROPERTIES_METHOD_NAME && method.returnType == Properties::class.java) {
        collectProperties("", Properties())
      } else {
        synchronized(map) {
          if (map[method] == null) {
            val path = prefix + method.name

            overrideProperty(path)

            map[method] = lazy { getValue(method, path, args) }
          }
          map[method]!!.value
        }
      }

    private fun overrideProperty(path: String): String {
      if (parents.isEmpty()) {
        val propertyValue = System.getProperty(path)
        if (propertyValue != null) {
          config = try {
            ConfigFactory.parseString("$path: $propertyValue").withFallback(config)
          } catch (ex: ConfigException.Parse) {
            throw error(path, "Syntax error", ex)
          }
        }
      }
      return path
    }

    private fun validateType(path: String, type: Class<*>) {
      if (type != Any::class.java) {
        if (!type.isInterface) {
          throw error(path, "${type.name} is not an interface or java.lang.Object")
        }

        for (method in type.methods) {
          if (method.name == PROPERTIES_METHOD_NAME && method.parameterCount == 0 && method.returnType == Properties::class.java) {
            continue
          }

          val newPath = (if (path.isEmpty()) "" else "$path.") + method.name
          if (method.isDefault) {
            throw error(newPath, "${method.toGenericString()}: default methods are not allowed")
          }
          if (Modifier.isStatic(method.modifiers)) {
            throw error(newPath, "${method.toGenericString()}: static methods are not allowed")
          }
          if (method.returnType.isArray) {
            throw error(newPath, "${method.toGenericString()}: ${java.util.List::class.java.name} should be used instead of ${method.returnType.name}")
          }
          if (method.returnType != java.util.List::class.java && Iterable::class.java.isAssignableFrom(method.returnType)) {
            throw error(newPath, "${method.toGenericString()}: ${java.util.List::class.java.name} should be used instead of ${method.returnType.name}")
          }
          if (method.returnType != java.util.Map::class.java && Map::class.java.isAssignableFrom(method.returnType)) {
            throw error(newPath, "${method.toGenericString()}: ${java.util.Map::class.java.name} should be used instead of ${method.returnType.name}")
          }
          if (method.returnType == java.util.List::class.java || method.returnType == java.util.Map::class.java) {
            if (method.parameterCount > 1) {
              throw error(newPath, "${method.toGenericString()} may have not more than 1 parameter")
            } else if (method.parameterCount == 1 && method.parameterTypes[0] != Class::class.java) {
              throw error(newPath, "parameter of ${method.toGenericString()} should be of java.lang.Class type")
            }
          } else {
            if (method.parameterCount > 0) {
              throw error(newPath, "${method.toGenericString()} may have no parameters")
            }
          }
        }
      }
    }

    private fun collectProperties(parent: String, properties: Properties): Properties {
      for (e in config.entrySet()) {
        val path = parent + e.key
        val value = when (e.value.valueType()!!) {
          ConfigValueType.BOOLEAN ->
            e.value.unwrapped().toString().toLowerCase()
          ConfigValueType.NUMBER ->
            e.value.unwrapped().toString()
          ConfigValueType.STRING ->
            e.value.unwrapped().toString()
          ConfigValueType.LIST ->
            null
          else ->
            throw IllegalStateException()
        }
        if (value != null && path.startsWith(prefix)) {
          properties.put(path.substring(prefix.length), value)
        }
      }
      return properties
    }

    private fun getValue(method: Method, path: String, args: Array<Any?>?): Any? =
      if (method.returnType == java.util.List::class.java) {
        if (config.hasPath(path)) {
          toList(path, config.getValue(path), getType(method, path, args))
        } else {
          null
        }
      } else if (method.returnType == java.util.Map::class.java) {
        if (config.hasPath(path)) {
          toMap(path, config.getValue(path), getType(method, path, args))
        } else {
          null
        }
      } else {
        if (config.hasPath(path)) {
          toValue(path, config.getValue(path), method.returnType)
        } else {
          if (method.returnType.isPrimitive) {
            if (config.hasPathOrNull(path)) {
              throw error(path, "value is null, but must be of type ${method.returnType.name}")
            } else {
              throw error(path, "value is undefined, but must be of type ${method.returnType.name}")
            }
          } else {
            null
          }
        }
      }

    private fun toValue(path: String, configValue: ConfigValue, type: Class<*>): Any =
      if (type == java.lang.Byte::class.java || type == Byte::class.java) {
        toNumber(path, configValue, Byte::class.java, { number -> number.byteValueExact() })
      } else if (type == java.lang.Short::class.java || type == Short::class.java) {
        toNumber(path, configValue, Short::class.java, { number -> number.shortValueExact() })
      } else if (type == java.lang.Integer::class.java || type == Int::class.java) {
        toNumber(path, configValue, Int::class.java, { number -> number.intValueExact() })
      } else if (type == java.lang.Long::class.java || type == Long::class.java) {
        toNumber(path, configValue, Long::class.java, { number -> number.longValueExact() })
      } else if (type == java.lang.Double::class.java || type == Double::class.java) {
        toNumber(path, configValue, Double::class.java, { number -> number.toDouble() })
      } else if (type == java.lang.Float::class.java || type == Float::class.java) {
        toNumber(path, configValue, Float::class.java, { number -> number.toFloat() })
      } else if (type == java.lang.Boolean::class.java || type == Boolean::class.java) {
        toValue(path, configValue, ConfigValueType.BOOLEAN, Boolean::class.java)
      } else if (type == java.lang.Character::class.java || type == Char::class.java) {
        toChar(path, configValue)
      } else if (type == java.lang.String::class.java) {
        toString(path, configValue)
      } else {
        if (type == Any::class.java) {
          when (configValue.valueType()) {
            ConfigValueType.NUMBER ->
              configValue.unwrapped()
            ConfigValueType.STRING ->
              configValue.unwrapped()
            ConfigValueType.BOOLEAN ->
              configValue.unwrapped()
            else ->
              throw IllegalStateException()
          }
        } else {
          if (configValue.valueType() == ConfigValueType.OBJECT) {
            Proxy.newProxyInstance(type.classLoader, arrayOf(type), Handler(type, path + ".", parents, config))
          } else {
            throw convertError(path, configValue, type)
          }
        }
      }

    private fun toMap(path: String, configObject: ConfigValue, type: Class<*>): Map<String, Any> {
      val map = hashMapOf<String, Any>()

      if (configObject.valueType() != ConfigValueType.OBJECT) {
        throw convertError(path, configObject, java.util.Map::class.java)
      }

      for (e in (configObject as ConfigObject).entries) {
        if (e.value != null && e.value.valueType() != ConfigValueType.NULL) {
          val newPath = (if (path.isEmpty()) "" else path + ".") + e.key;
          val element = if (e.value.valueType() == ConfigValueType.LIST) {
            toList(newPath, e.value as ConfigList, type)
          } else if (e.value.valueType() == ConfigValueType.OBJECT) {
            if (canBeProxied(type)) {
              Proxy.newProxyInstance(type.classLoader, arrayOf(type), Handler(type, newPath + ".", parents, config))
            } else {
              toMap(newPath, e.value as ConfigObject, type)
            }
          } else {
            toValue(newPath, e.value, type)
          }
          if (element != null) {
            map.put(e.key, element)
          }
        }
      }

      return map;
    }

    private fun toList(path: String, configList: ConfigValue, type: Class<*>): List<Any?> {
      val result = arrayListOf<Any?>()

      if (configList.valueType() != ConfigValueType.LIST) {
        throw convertError(path, configList, java.util.List::class.java)
      }

      for (configValue in (configList as ConfigList)) {
        if (configValue != null && configValue.valueType() != ConfigValueType.NULL) {
          val element = if (configValue.valueType() == ConfigValueType.LIST) {
            toList(path, configValue as ConfigList, type)
          } else if (configValue.valueType() == ConfigValueType.OBJECT) {
            val handler = Handler(type, "", listOf(path) + parents, (configValue as ConfigObject).toConfig())
            if (canBeProxied(type)) {
              Proxy.newProxyInstance(type.classLoader, arrayOf(type), handler)
            } else {
              handler.toMap("", configValue, type)
            }
          } else {
            toValue(path, configValue, type)
          }
          result.add(element)
        } else {
          result.add(null)
        }
      }

      return result
    }

    private fun <T> toNumber(path: String, configValue: ConfigValue, type: Class<T>, transform: (BigDecimal) -> T): T {
      val value = toValue(path, configValue, ConfigValueType.NUMBER, type)
      return try {
        when (value) {
          is Int ->
            transform(BigDecimal(value))
          is Long ->
            transform(BigDecimal(value))
          is Double ->
            transform(BigDecimal(value))
          else ->
            throw IllegalStateException()
        }
      } catch(ex: ArithmeticException) {
        throw convertError(path, configValue, type)
      }
    }

    private fun toString(path: String, configValue: ConfigValue) =
      toValue(path, configValue, ConfigValueType.STRING, String::class.java) as String

    private fun toChar(path: String, configValue: ConfigValue): Char =
      when (configValue.valueType()) {
        ConfigValueType.STRING -> {
          val string = configValue.unwrapped() as String
          if (string.length == 1) {
            string.toCharArray()[0]
          } else {
            throw convertError(path, configValue, Char::class.java)
          }
        }
        else ->
          throw this.convertError(path, configValue, Char::class.java)
      }

    private fun toValue(path: String, configValue: ConfigValue, configValueType : ConfigValueType, type: Class<*>): Any =
      if (configValue.valueType() == configValueType) {
        configValue.unwrapped()
      } else {
        throw this.convertError(path, configValue, type)
      }

    private fun getType(method: Method, path: String, args: Array<Any?>?): Class<*> =
      if (method.parameterCount == 0) {
        Any::class.java
      } else if ((args!![0] as Class<*>).isArray) {
        throw error(path, "type parameter cannot be an array")
      } else if (Iterable::class.java.isAssignableFrom(args[0] as Class<*>)) {
        throw error(path, "type parameter cannot be ${java.lang.Iterable::class.java.name}")
      } else if (Map::class.java.isAssignableFrom(args[0] as Class<*>)) {
        throw error(path, "type parameter cannot be ${java.util.Map::class.java.name}")
      } else {
        args[0] as Class<*>
      }

    private fun canBeProxied(type: Class<*>): Boolean {
      return !(type == java.lang.Byte::class.java || type == Byte::class.java
        || type == java.lang.Short::class.java || type == Short::class.java
        || type == java.lang.Integer::class.java || type == Int::class.java
        || type == java.lang.Long::class.java || type == Long::class.java
        || type == java.lang.Double::class.java || type == Double::class.java
        || type == java.lang.Float::class.java || type == Float::class.java
        || type == java.lang.Boolean::class.java || type == Boolean::class.java
        || type == java.lang.Character::class.java || type == Char::class.java
        || type == java.lang.String::class.java
        || type == Any::class.java)
    }

    private fun error(path: String, message: String, ex: Exception? = null): IllegalArgumentException {
      val fullPath = parents.fold(path, { result, element ->
        "$element[$result]"
      })
      return IllegalArgumentException("$fullPath${if (fullPath.isEmpty()) "" else ": "}$message", ex)
    }

    private fun convertError(path: String, configValue: ConfigValue, dstType: Class<*>, ex: Exception? = null): IllegalArgumentException {
      val srcType = when(configValue.valueType()) {
        ConfigValueType.BOOLEAN ->
          Boolean::class.java
        ConfigValueType.STRING ->
          String::class.java
        ConfigValueType.NUMBER ->
          when (configValue.unwrapped()) {
            is Int ->
              Int::class.java
            is Long ->
              Long::class.java
            is Double ->
              Double::class.java
            else ->
              throw IllegalStateException(ex)
          }
        ConfigValueType.LIST ->
          List::class.java
        ConfigValueType.OBJECT ->
          Any::class.java
        else ->
          throw IllegalStateException(ex)
      }
      return error(path, "cannot convert ${srcType.name} to ${dstType.name}", ex)
    }
  }
}
