# Konfiguration

Configuration factory creates proxies for configuration interfaces.
This is used to map an application configuration onto a tree of objects.
It is allowed that some configuration properties are not mapped.
If method return type is nullable, missing configuration property is represented as a null value.

## Configuration interfaces

Configuration interface is a Java interface which has no default or static methods.
Methods have no parameters and can return

* byte or java.lang.Byte
* short or java.lang.Short
* int or java.lang.Integer
* long or java.lang.Long
* double or java.lang.Double
* float or java.lang.Float
* boolean or java.lang.Boolean
* char or java.lang.Character
* java.lang.String
* java.lang.List
* java.lang.Map

### List and map methods
Methods that return lists or maps are allowed to have one (optional) parameter of java.lang.Class type which defines the type for map or list elements.
Returned lists or maps can be of any depth as used by the configuration, that is, elements of lists or maps can be lists or maps themselves.

### properties() method
An interface may have an optional properties method which should return java.lang.Properties and in this case cannot have parameters.
(If it returns values of some other type, general rules are applied and these methods are not treated as properties methods.)
Properties methods are used to convert trees of objects into properties.
The produced properties will have all configuration properties even if they are not specified by appropriate interfaces.
The returned properties are relative to the object on which the method is called.
The returned properties will have no lists, such values are bypassed.

## Configuration files
Configuration files are written in HOCON format.
A library may provide its configuration defaults in jarKonfiguration.props file which should be on the classpath.
Library defaults may be overridden by the application wide configuration.
The application configuration may be put in any file on the classpath or on the filesystem.
Which file to use for the application configuration is defined by a system property.
The name of the system property is defined when the #load method is called.
One possible configuration usage is to initialize the configuration tree at start-up of the application
and then provide the tree (or its branches) to the appropriate application modules.
This way the name of the system property is defined by the application developers
but the definition of the system property value is a separate process and may be achieved in different ways as well.
For example, it may be passed as a parameter to the application executable on its launch or set in the code.
The application developers should document the name of the system property and may prepare application configuration defaults
in a separate file located on the classpath or on the filesystem.
Deployment manager then may use the defaults as an example and prepare their own application wide configuration.
For example, if the application initialization process allows setting the mentioned above system property outside the code,
the deployment manager then will have an ability to choose their own configuration file or the one provided by the application developers.
Please notice the application wide configuration should be located in one file, it's not allowed to use multiple file,
an none of them overrides the other. Application wide configuration files do not override each other since only one of them is used,
but it can override libraries' defaults.

## System properties
The user also can override application and library defaults by setting system properties.
This won't work for array-like properties since there is no way to specify multiple values for one property.
If the system properties are not set in the code then they may be passed as properties to the JVM in any suitable manner,
for example, calling from Bash:

* -Dnet.ofk.kplatform.initMode=```"fast and dirty"```

  the value **fast and dirty** will be interpreted as string since it has spaces, and cannot be converted to a boolean or number
* -Dnet.ofk.kplatform.delay=```1000```

  the value **1000** will be interpreted as int
* -Dnet.ofk.kplatform.email=```\"ma\\\"il@example.com\"```

  the value ```ma"il@example.com``` will be interpreted as a string since we explicitly use double quotes here, the starting and ending double quotes are not parts of the property value,
  the property value will have one double-quote between **ma** and **il**
