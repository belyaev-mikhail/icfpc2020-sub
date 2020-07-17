package ru.spbstu

import org.junit.Assert.assertEquals
import org.junit.Test

class SymbolTest {
    @Test
    fun `test numbers creation`() {
        assertEquals(0, Symbols.number(" .|. ").code)
        assertEquals(1, Symbols.number(" .|..").code)
        assertEquals(2, Symbols.number(" ..|. .|.  ").code)
        assertEquals(3, Symbols.number(" ..|...|.  ").code)
        assertEquals(4, Symbols.number(" ..|.  |.. ").code)
        assertEquals(5, Symbols.number(" ..|.. |.. ").code)
        assertEquals(6, Symbols.number(" ..|. .|.. ").code)
        assertEquals(7, Symbols.number(" ..|...|.. ").code)
        assertEquals(8, Symbols.number(" ..|.  |. .").code)
    }

    @Test
    fun `test negative numbers creation`() {
        assertEquals(-0, Symbols.number(" .|. |. ").code)
        assertEquals(-1, Symbols.number(" .|..|. ").code)
        assertEquals(-2, Symbols.number(" ..|. .|.  |.  ").code)
        assertEquals(-3, Symbols.number(" ..|...|.  |.  ").code)
        assertEquals(-4, Symbols.number(" ..|.  |.. |.  ").code)
        assertEquals(-5, Symbols.number(" ..|.. |.. |.  ").code)
        assertEquals(-6, Symbols.number(" ..|. .|.. |.  ").code)
        assertEquals(-7, Symbols.number(" ..|...|.. |.  ").code)
        assertEquals(-8, Symbols.number(" ..|.  |. .|.  ").code)
    }

    @Test
    fun `test variables creation`() {
        assertEquals("x0", Symbols.variable("....|.. .|. ..|....").command)
        assertEquals("x1", Symbols.variable("....|.. .|.  .|....").command)
        assertEquals("x2", Symbols.variable(".....|..  .|. . .|. ...|.....").command)
        assertEquals("x3", Symbols.variable(".....|..  .|.   .|. ...|.....").command)
        assertEquals("x4", Symbols.variable(".....|..  .|. ...|.  ..|.....").command)
    }

    @Test
    fun `test functions creation`() {
        assertEquals(":14", Symbols.function("...|. .|...").command)
        assertEquals(":10", Symbols.function("...|. .|. .").command)
        assertEquals(":7", Symbols.function("...|...|.. ").command)
        assertEquals(":6", Symbols.function("...|. .|.. ").command)
        assertEquals(":5", Symbols.function("...|.. |.. ").command)
        assertEquals(":68191693600", Symbols.function("""
            |.......
            |.     .
            |.  .. .
            |. . . .
            |. .   .
            |.     .
            |.......""".trimMargin()).command)
        assertEquals(":64170", Symbols.function("""
            |.....
            |. . .
            |. . .
            |. . .
            |.....""".trimMargin()).command)
        assertEquals(":64174", Symbols.function("""
            |.....
            |. ...
            |. . .
            |. . .
            |.....""".trimMargin()).command)
        assertEquals(":64171", Symbols.function("""
            |.....
            |... .
            |. . .
            |. . .
            |.....""".trimMargin()).command)
        assertEquals(":17043521", Symbols.function("""
            |......
            |..    
            |. .   
            |.  .  
            |.   . 
            |.    .""".trimMargin()).command)
    }

    @Test
    fun `test custom creation`() {
        assertEquals("(", Symbols.custom("  .| ..|...| ..|  .").command)
        assertEquals(")", Symbols.custom(".  |.. |...|.. |.  ").command)
        assertEquals("|", Symbols.custom("..|..|..|..|..").command)
        assertEquals("universe", Symbols.custom("""
            |  ...  
            |     . 
            | ...  .
            |. . . .
            |.  ... 
            | .     
            |  ...  """.trimMargin()).command)
    }
}