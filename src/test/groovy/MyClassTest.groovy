import spock.lang.Specification

class MyClassTest extends Specification {
    def "sayHello should return proper greeting"() {
        given:
        def myClass = new MyClass()

        when:
        def result = myClass.sayHello("John")

        then:
        result == "Hello, John!"
    }

    def "sayHello works with different names"() {
        given:
        def myClass = new MyClass()

        expect:
        myClass.sayHello(name) == expected

        where:
        name     | expected
        "Alice"  | "Hello, Alice!"
        "Bob"    | "Hello, Bob!"
        "Carol"  | "Hello, Carol!"
    }

    def "demonstrate mocking capabilities"() {
        given:
        def mockClass = Mock(MyClass)
        mockClass.sayHello("John") >> "Mocked hello, John!"

        when:
        def result = mockClass.sayHello("John")

        then:
        result == "Mocked hello, John!"
    }
}