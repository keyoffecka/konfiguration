package net.ofk.konfiguration

import net.ofk.kutils.Auto
import org.junit.After
import org.junit.Assert
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader
import java.io.StringWriter
import java.lang.reflect.Proxy
import java.util.ArrayList
import java.util.HashMap
import java.util.Properties
import java.util.stream.Collectors

class KonfigurationFactoryTest {
  @After
  fun setDown() {
    System.clearProperty("byte")
    System.clearProperty("short")
    System.clearProperty("int")
    System.clearProperty("long")
    System.clearProperty("float")
    System.clearProperty("double")
    System.clearProperty("char")
    System.clearProperty("string")
    System.clearProperty("boolean")

    System.clearProperty("cc.byte")
    System.clearProperty("cc.short")
    System.clearProperty("cc.int")
    System.clearProperty("cc.long")
    System.clearProperty("cc.float")
    System.clearProperty("cc.double")
    System.clearProperty("cc.char")
    System.clearProperty("cc.boolean")
    System.clearProperty("cc")
    System.clearProperty("list")
    System.clearProperty("list2")
    System.clearProperty("map")
    System.clearProperty("map2")

    System.clearProperty("b")
  }

  @Test
  fun overrideTest() {
    val c = KonfigurationFactory().load(C::class.java) as Object
    val c2 = KonfigurationFactory().load(C::class.java) as Object
    Assert.assertEquals(Proxy.getInvocationHandler(c).hashCode() xor 32, c.hashCode())
    Assert.assertEquals(c.javaClass.name, c.toString())
    Assert.assertTrue(c.equals(c))
    Assert.assertFalse(c.equals(c2))

    var d = KonfigurationFactory().load(D::class.java)
    Assert.assertEquals(1, d.bb());

    System.setProperty("cfg", "./src/test/resources/App.props")
    d = KonfigurationFactory().load(D::class.java, "cfg")
    Assert.assertEquals(2, d.bb());

    System.setProperty("cfg", "classpath:App2.props")
    d = KonfigurationFactory().load(D::class.java, "cfg")
    Assert.assertEquals(4, d.bb());

    System.setProperty("bb", "3")
    d = KonfigurationFactory().load(D::class.java, "cfg")
    Assert.assertEquals(3, d.bb());
  }

  @Test
  fun testNullableAndNotNullable() {
    val c = KonfigurationFactory().load(C::class.java)
    Assert.assertNull(c.byte())
    Assert.assertNull(c.short())
    Assert.assertNull(c.int())
    Assert.assertNull(c.long())
    Assert.assertNull(c.float())
    Assert.assertNull(c.double())
    Assert.assertNull(c.boolean())
    Assert.assertNull(c.char())
    Assert.assertNull(c.string())

    System.setProperty("cc", "{}");

    try {
      c.cc()!!.byte()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("cc.byte: value is undefined, but must be of type byte", ex.message)
    }
    try {
      c.cc()!!.short()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("cc.short: value is undefined, but must be of type short", ex.message)
    }
    try {
      c.cc()!!.int()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("cc.int: value is undefined, but must be of type int", ex.message)
    }
    try {
      c.cc()!!.long()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("cc.long: value is undefined, but must be of type long", ex.message)
    }
    try {
      c.cc()!!.float()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("cc.float: value is undefined, but must be of type float", ex.message)
    }
    try {
      c.cc()!!.double()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("cc.double: value is undefined, but must be of type double", ex.message)
    }
    try {
      c.cc()!!.boolean()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("cc.boolean: value is undefined, but must be of type boolean", ex.message)
    }
    try {
      c.cc()!!.char()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("cc.char: value is undefined, but must be of type char", ex.message)
    }
  }

  @Test
  fun testBadConverters() {
    System.setProperty("short", "32768")
    System.setProperty("int", "2147483648")
    System.setProperty("long", "1.1")
    System.setProperty("float", "a")
    System.setProperty("double", "b")
    System.setProperty("boolean", "\"true\"")
    System.setProperty("string", "true")
    System.setProperty("char", "xyz")
    System.setProperty("list", "1")
    var c = KonfigurationFactory().load(C::class.java)

    System.setProperty("byte", "256")
    try {
      c.byte()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("byte: cannot convert int to byte", ex.message)
    }
    try {
      c.short()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("short: cannot convert int to short", ex.message)
    }
    try {
      c.int()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("int: cannot convert long to int", ex.message)
    }
    try {
      c.long()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("long: cannot convert double to long", ex.message)
    }
    try {
      c.float()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("float: cannot convert java.lang.String to float", ex.message)
    }
    try {
      c.double()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("double: cannot convert java.lang.String to double", ex.message)
    }
    try {
      c.boolean()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("boolean: cannot convert java.lang.String to boolean", ex.message)
    }
    try {
      c.char()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("char: cannot convert java.lang.String to char", ex.message)
    }
    try {
      c.string()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("string: cannot convert boolean to java.lang.String", ex.message)
    }
    try {
      c.list()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("list: cannot convert int to java.util.List", ex.message)
    }

    System.setProperty("char", "1")
    c = KonfigurationFactory().load(C::class.java)
    try {
      c.char()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("char: cannot convert int to char", ex.message)
    }

    System.setProperty("char", "{a: \"x\"}")
    c = KonfigurationFactory().load(C::class.java)
    try {
      c.char()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("char: cannot convert java.lang.Object to char", ex.message)
    }

    System.setProperty("cc", "{char: null}")
    c = KonfigurationFactory().load(C::class.java)
    try {
      c.cc()!!.char()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("cc.char: value is null, but must be of type char", ex.message)
    }

    System.setProperty("cc", "[]")
    c = KonfigurationFactory().load(C::class.java)
    try {
      c.cc()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("cc: cannot convert java.util.List to net.ofk.konfiguration.CC", ex.message)
    }
  }

  @Test
  fun testConverters() {
    System.setProperty("cc", "{}");
    System.setProperty("cc.byte", "127")
    System.setProperty("cc.short", "32767")
    System.setProperty("cc.int", "2147483647")
    System.setProperty("cc.long", "2147483648")
    System.setProperty("cc.float", "1.1")
    System.setProperty("cc.double", "1.2")
    System.setProperty("cc.boolean", "true")
    System.setProperty("string", "\"true\"")
    System.setProperty("cc.char", "x")

    var c = KonfigurationFactory().load(C::class.java)
    val cc = c.cc()!!

    Assert.assertEquals(java.lang.Byte(127), cc.byte());
    Assert.assertEquals(java.lang.Short(32767), cc.short());
    Assert.assertEquals(2147483647, cc.int());
    Assert.assertEquals(2147483648, cc.long());
    Assert.assertEquals(1.1f, cc.float(), 0.001f);
    Assert.assertEquals(1.2, cc.double(), 0.001);
    Assert.assertTrue(cc.boolean());
    Assert.assertEquals("true", c.string());
    Assert.assertEquals('x', cc.char());

    System.setProperty("string", "null")
    c = KonfigurationFactory().load(C::class.java)
    Assert.assertNull(c.string());
    Assert.assertNull(c.list());

    System.setProperty("list", "[1]")
    c = KonfigurationFactory().load(C::class.java)
    Assert.assertEquals(listOf(1), c.list());

    System.setProperty("map2", "null")
    c = KonfigurationFactory().load(C::class.java)
    Assert.assertNull(c.map2())

    System.setProperty("list", "[")
    c = KonfigurationFactory().load(C::class.java)
    try {
      c.list()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("list: Syntax error", ex.message)
    }

    System.setProperty("list2", "[]")

    c = KonfigurationFactory().load(C::class.java)
    try {
      c.list2(List::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("list2: type parameter cannot be java.lang.Iterable", ex.message)
    }

    c = KonfigurationFactory().load(C::class.java)
    try {
      c.list2(Map::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("list2: type parameter cannot be java.util.Map", ex.message)
    }

    System.setProperty("map2", "{}")

    c = KonfigurationFactory().load(C::class.java)
    try {
      c.map2(List::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("map2: type parameter cannot be java.lang.Iterable", ex.message)
    }

    c = KonfigurationFactory().load(C::class.java)
    try {
      c.map2(Map::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("map2: type parameter cannot be java.util.Map", ex.message)
    }

    try {
      KonfigurationFactory().load(B1::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("alist: public abstract java.util.ArrayList<?> net.ofk.konfiguration.B1.alist(): java.util.List should be used instead of java.util.ArrayList", ex.message)
    }

    try {
      KonfigurationFactory().load(B2::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("hmap: public abstract java.util.HashMap<?, ?> net.ofk.konfiguration.B2.hmap(): java.util.Map should be used instead of java.util.HashMap", ex.message)
    }

    try {
      KonfigurationFactory().load(B3::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("net.ofk.konfiguration.B3 is not an interface or java.lang.Object", ex.message)
    }

    try {
      KonfigurationFactory().load(B4::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("b: public abstract java.lang.Integer net.ofk.konfiguration.B4.b(int,int) may have no parameters", ex.message)
    }

    try {
      KonfigurationFactory().load(B6::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("b: public abstract java.lang.Integer[] net.ofk.konfiguration.B6.b(): java.util.List should be used instead of [Ljava.lang.Integer;", ex.message)
    }

    try {
      KonfigurationFactory().load(B7::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("b: public default java.lang.String net.ofk.konfiguration.B7.b(): default methods are not allowed", ex.message)
    }

    try {
      KonfigurationFactory().load(B8::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("b: public static java.lang.String net.ofk.konfiguration.B8.b(): static methods are not allowed", ex.message)
    }

    try {
      KonfigurationFactory().load(B9::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("b: parameter of public abstract java.util.List<java.lang.Integer> net.ofk.konfiguration.B9.b(int) should be of java.lang.Class type", ex.message)
    }

    val b5 = KonfigurationFactory().load(B5::class.java)
    System.setProperty("b", "[]")
    try {
      b5.b(Array<Int?>::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("b: type parameter cannot be an array", ex.message)
    }

    System.setProperty("map", "1")
    try {
      c.map()
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("map: cannot convert int to java.util.Map", ex.message)
    }
  }

  @Test
  fun testList() {
    System.setProperty("list", "[[], [], null]")
    var c = KonfigurationFactory().load(C::class.java)
    Assert.assertEquals(listOf(listOf(), listOf(), null as List<Int>?), c.list())

    System.setProperty("list", "[[1], [[a],[b]]]")
    c = KonfigurationFactory().load(C::class.java)
    Assert.assertEquals(listOf(listOf(1), listOf(listOf("a"), listOf("b"))), c.list());

    System.setProperty("list", "[[[[]]], [[true],[b]]]")
    c = KonfigurationFactory().load(C::class.java)
    Assert.assertEquals(listOf(listOf(listOf<Any>(listOf<Any>())), listOf(listOf(true), listOf("b"))), c.list());

    System.setProperty("list", "[{}, [[[]]], [{}]]")
    c = KonfigurationFactory().load(C::class.java)
    Assert.assertEquals(listOf(mapOf<String, Any>(), listOf(listOf<Any>(listOf<Any>())), listOf(mapOf<String, Any>())), c.list());

    System.setProperty("list", "[{a: 1}, [[[]]], [{a: 2}]]")
    c = KonfigurationFactory().load(C::class.java)
    Assert.assertEquals(listOf(mapOf("a" to 1), listOf(listOf<Any>(listOf<Any>())), listOf(mapOf("a" to 2))), c.list());

    System.setProperty("list2", "[[{a: 1}, {a: 2}], [{a: 3}, {a: 4}]]")
    c = KonfigurationFactory().load(C::class.java)
    Assert.assertEquals(1.0f, c.list2(A::class.java)!![0]!![0]!!.a()!!, 0.0001f)
    Assert.assertEquals(2.0f, c.list2(A::class.java)!![0]!![1]!!.a()!!, 0.0001f)
    Assert.assertEquals(3.0f, c.list2(A::class.java)!![1]!![0]!!.a()!!, 0.0001f)
    Assert.assertEquals(4.0f, c.list2(A::class.java)!![1]!![1]!!.a()!!, 0.0001f)

    System.setProperty("list2", "[[{l: [{b: 1}]}, {l: [{b: 2}]}], [{l: [{b: 3}]}, {l: [{b: 4}]}]]")
    c = KonfigurationFactory().load(C::class.java)
    Assert.assertEquals(1, c.list2(A::class.java)!![0]!![0]!!.l(A::class.java)!![0]!!.b())
    Assert.assertEquals(2, c.list2(A::class.java)!![0]!![1]!!.l(A::class.java)!![0]!!.b())
    Assert.assertEquals(3, c.list2(A::class.java)!![1]!![0]!!.l(A::class.java)!![0]!!.b())
    Assert.assertEquals(4, c.list2(A::class.java)!![1]!![1]!!.l(A::class.java)!![0]!!.b())

    Assert.assertEquals(1, c.list2()!![0]!![0]!!["l"]!![0]!!["b"])
    Assert.assertEquals(2, c.list2()!![0]!![1]!!["l"]!![0]!!["b"])
    Assert.assertEquals(3, c.list2()!![1]!![0]!!["l"]!![0]!!["b"])
    Assert.assertEquals(4, c.list2()!![1]!![1]!!["l"]!![0]!!["b"])

    System.setProperty("list2", "[[{l: [{b: null}]}, {l: [{b: 2}]}], [{l: [{b: 3}]}, {l: [{b: 4}]}]]")
    c = KonfigurationFactory().load(C::class.java)

    try {
      c.list2(A::class.java)!![0]!![0]!!.l(A::class.java)!![0]!!.b()
      Assert.fail()
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("list2[l[b]]: value is null, but must be of type int", ex.message)
    }

    System.setProperty("list2", "[1]")
    c = KonfigurationFactory().load(C::class.java)

    try {
      c.list2(A::class.java)
      Assert.fail();
    } catch(ex: IllegalArgumentException) {
      Assert.assertEquals("list2: cannot convert int to net.ofk.konfiguration.A", ex.message)
    }
  }

  @Test
  fun testMap() {
    System.setProperty("map", "{a: 1, l: [2, 3], b: null, c:{x:\"i\"}, ll: [{y:90, lll:[1, {f:-1}]}, null, []]}")
    var c = KonfigurationFactory().load(C::class.java)

    Assert.assertNull(c.map()!!["xx"])
    Assert.assertNull(c.map()!!["b"])
    Assert.assertEquals(1, c.map()!!["a"])
    Assert.assertEquals(listOf(2, 3), c.map()!!["l"])
    Assert.assertEquals("i", (c.map()!!["c"] as Map<String, String>?)!!["x"])
    val ll = (c.map()!!["ll"] as List<*>)
    val ll0 = ll[0] as Map<String, *>
    Assert.assertEquals(90, ll0["y"])
    val lll = ll0["lll"] as List<*>
    Assert.assertEquals(1, lll[0])
    val lll1 = lll[1] as Map<String, *>
    Assert.assertEquals(-1, lll1["f"])
    Assert.assertNull(ll[1])
    Assert.assertEquals(listOf<Any>(), ll[2])

    System.setProperty("map2", "{a:{b1:[{c1:[{l:[true, false]}, {l:[true]}, {c2: null}]}], b2:[{c3:[{l:[false, true]}, {l:[false]}, {c2: []}]}]}, b:{}}")
    c = KonfigurationFactory().load(C::class.java)
    Assert.assertTrue(c.map2(A::class.java)!!["a"]!!.b1(A::class.java)!![0]!!.c1(A::class.java)!![0]!!.l(Boolean::class.java)!![0]!!)
    Assert.assertFalse(c.map2(A::class.java)!!["a"]!!.b1(A::class.java)!![0]!!.c1(A::class.java)!![0]!!.l(Boolean::class.java)!![1]!!)
    Assert.assertTrue(c.map2(A::class.java)!!["a"]!!.b1(A::class.java)!![0]!!.c1(A::class.java)!![1]!!.l(Boolean::class.java)!![0]!!)
    Assert.assertNull(c.map2(A::class.java)!!["a"]!!.b1(A::class.java)!![0]!!.c1(A::class.java)!![2]!!.l(Boolean::class.java))
    Assert.assertNull(c.map2(A::class.java)!!["a"]!!.b1(A::class.java)!![0]!!.c2(A::class.java))
    Assert.assertFalse(c.map2(A::class.java)!!["a"]!!.b2(A2::class.java)!![0]!!.c3(A::class.java)!![0]!!.l(Boolean::class.java)!![0]!!)
    Assert.assertTrue(c.map2(A::class.java)!!["a"]!!.b2(A2::class.java)!![0]!!.c3(A::class.java)!![0]!!.l(Boolean::class.java)!![1]!!)
    Assert.assertFalse(c.map2(A::class.java)!!["a"]!!.b2(A2::class.java)!![0]!!.c3(A::class.java)!![1]!!.l(Boolean::class.java)!![0]!!)
    Assert.assertTrue(c.map2(A::class.java)!!["a"]!!.b2(A2::class.java)!![0]!!.c3(A::class.java)!![2]!!.c2(A::class.java)!!.isEmpty())
    Assert.assertEquals(1, c.map2(A::class.java)!!["a"]!!.b1(A::class.java)!!.size)
    Assert.assertEquals(3, c.map2(A::class.java)!!["a"]!!.b1(A::class.java)!![0]!!.c1(A::class.java)!!.size)
    Assert.assertEquals(1, c.map2(A::class.java)!!["a"]!!.b2(A2::class.java)!!.size)
    Assert.assertEquals(3, c.map2(A::class.java)!!["a"]!!.b2(A2::class.java)!![0]!!.c3(A::class.java)!!.size)
    Assert.assertEquals(2, c.map2(A::class.java)!!["a"]!!.b2(A2::class.java)!![0]!!.c3(A::class.java)!![0]!!.l(Boolean::class.java)!!.size)
    Assert.assertEquals(1, c.map2(A::class.java)!!["a"]!!.b2(A2::class.java)!![0]!!.c3(A::class.java)!![1]!!.l(Boolean::class.java)!!.size)
    Assert.assertEquals(mapOf<String, Any>(), c.map2()!!["b"])

    Assert.assertTrue(((((c.map2()!!["a"]!!["b1"] as List<*>)[0] as Map<*, *>)["c1"] as List<*>)[0] as Map<String, List<Boolean>>)["l"]!![0])
    Assert.assertFalse(((((c.map2()!!["a"]!!["b1"] as List<*>)[0] as Map<*, *>)["c1"] as List<*>)[0] as Map<String, List<Boolean>>)["l"]!![1])
    Assert.assertTrue(((((c.map2()!!["a"]!!["b1"] as List<*>)[0] as Map<*, *>)["c1"] as List<*>)[1] as Map<String, List<Boolean>>)["l"]!![0])
    Assert.assertNull(((((c.map2()!!["a"]!!["b1"] as List<*>)[0] as Map<*, *>)["c1"] as List<*>)[2] as Map<String, List<Boolean>>)["l"])
    Assert.assertNull(((((c.map2()!!["a"]!!["b1"] as List<*>)[0] as Map<*, *>)["c2"])))
    Assert.assertFalse(((((c.map2()!!["a"]!!["b2"] as List<*>)[0] as Map<*, *>)["c3"] as List<*>)[0] as Map<String, List<Boolean>>)["l"]!![0])
    Assert.assertTrue(((((c.map2()!!["a"]!!["b2"] as List<*>)[0] as Map<*, *>)["c3"] as List<*>)[0] as Map<String, List<Boolean>>)["l"]!![1])
    Assert.assertFalse(((((c.map2()!!["a"]!!["b2"] as List<*>)[0] as Map<*, *>)["c3"] as List<*>)[1] as Map<String, List<Boolean>>)["l"]!![0])
    Assert.assertTrue(((((c.map2()!!["a"]!!["b2"] as List<*>)[0] as Map<*, *>)["c3"] as List<*>)[2] as Map<String, List<Boolean>>)["c2"]!!.isEmpty())
    Assert.assertEquals(1, (c.map2()!!["a"]!!["b1"] as List<*>).size)
    Assert.assertEquals(3, (((c.map2()!!["a"]!!["b1"] as List<*>)[0] as Map<*, *>)["c1"] as List<*>).size)
    Assert.assertEquals(1, (c.map2()!!["a"]!!["b2"] as List<*>).size)
    Assert.assertEquals(3, (((c.map2()!!["a"]!!["b2"] as List<*>)[0] as Map<*, *>)["c3"] as List<*>).size)
    Assert.assertEquals(2, ((((c.map2()!!["a"]!!["b2"] as List<*>)[0] as Map<*, *>)["c3"] as List<*>)[0] as Map<String, List<Boolean>>)["l"]!!.size)
    Assert.assertEquals(1, ((((c.map2()!!["a"]!!["b2"] as List<*>)[0] as Map<*, *>)["c3"] as List<*>)[1] as Map<String, List<Boolean>>)["l"]!!.size)
  }

  @Test
  fun testProperties() {
    System.setProperty("cfg", "classpath:App3.props")
    val c = KonfigurationFactory().load(DD::class.java, "cfg")
    assertPropertiesEqual("all.properties", c.properties())
    assertPropertiesEqual("a.properties", c.a()!!.properties())
    assertPropertiesEqual("b.properties", c.a()!!.b()!!.properties())
  }

  private fun assertPropertiesEqual(path: String, properties: Properties) {
    val sw = Auto.close {
      val sw = StringWriter().open()
      properties.store(sw, null)
      sw
    }

    val text = Auto.close {
      val l = BufferedReader(StringReader(sw.toString())).open()

      val it = l!!.lines().iterator()
      it.next()

      var text = ""
      val ll = listOf<String>() + it.asSequence()
      for (line in ll.sorted()) {
        text += "$line\n"
      }
      if (!text.isEmpty()) {
        text = text.substring(0, text.length - 1)
      }
      text
    }

    Assert.assertEquals(
      BufferedReader(InputStreamReader(this.javaClass.classLoader.getResourceAsStream(path))).lines().collect(Collectors.toList()).fold("") {all, el ->
        if (all.isEmpty()) "$el" else "$all\n$el"
      },
      text
    )
  }
}

interface B1 {
  fun alist(): ArrayList<*>?
}

interface B2 {
  fun hmap(): HashMap<*, *>?
}

abstract class B3 {
  abstract fun b(): Int?
}

interface B4 {
  fun b(a: Int, b: Int): Int?
}

interface B5 {
  fun b(type: Class<Array<Int?>>): List<Array<Int?>>
}

interface B6 {
  fun b(): Array<Int>?
}

interface B9 {
  fun b(type: Int): List<Int?>?
}

interface D {
  fun bb(): Int?
}

interface DD {
  fun bb(): Int?
  fun a(): BB?

  fun properties(): Properties
}

interface BB {
  fun b(): BBB?
  fun properties(): Properties
}

interface BBB {
  fun properties(): Properties
}

interface C {
  fun byte(): Byte?
  fun short(): Short?
  fun int(): Int?
  fun long(): Long?
  fun float(): Float?
  fun double(): Double?
  fun boolean(): Boolean?
  fun char(): Char?
  fun string(): String?

  fun cc(): CC?
  fun list(): List<*>?
  fun list2(): List<List<Map<String, List<Map<String, *>?>?>?>?>?
  fun <T> list2(type: Class<T>): List<List<T?>?>?
  fun map(): Map<String, *>?
  fun <T> map2(type: Class<T>): Map<String, T>?
  fun map2(): Map<String, Map<String, List<Map<String, List<Boolean>>>>>?
}

interface A {
  fun a(): Float?
  fun b(): Int
  fun <T> b1(type: Class<T>): List<T?>?
  fun <T> b2(type: Class<T>): List<T?>?
  fun c1(type: Class<A>): List<A?>?
  fun c2(type: Class<A>): List<A?>?
  fun <T> l(type: Class<T>): List<T?>?
}

interface A2{
  fun c3(type: Class<A>): List<A?>?
}

interface CC {
  fun byte(): Byte
  fun short(): Short
  fun int(): Int
  fun long(): Long
  fun float(): Float
  fun double(): Double
  fun boolean(): Boolean
  fun char(): Char
}