package org.github.erdi.javax.json

import spock.lang.Specification

import javax.json.spi.JsonProvider

class JsonPointerJsonParserSpec extends Specification {

    JsonPointerJsonParser parser

    def "empty pointer is returned before any parsing events are consumed"() {
        given:
        json ''

        expect:
        parser.pointer == ""
    }

    def "pointers are returned as expected when parsing an empty json object"() {
        given:
        json '{}'

        expect:
        allPointers == ['', '/', '']
    }

    def "pointers are returned as expected when parsing an empty json array"() {
        given:
        json '[]'

        expect:
        allPointers == ['', '/', '']
    }

    def "pointers are returned as expected when parsing a json object with property of various primitive types"() {
        given:
        json '''
            {
                "first": "foo",
                "second": 2,
                "third": true,
                "fourth": false,
                "fifth": null
            }
        '''

        expect:
        allPointers == [
            '',
            '/',
            '/',
            '/first',
            '/',
            '/second',
            '/',
            '/third',
            '/',
            '/fourth',
            '/',
            '/fifth',
            ''
        ]
    }

    def "pointers are returned as expected when parsing a json array with values of various primitive types"() {
        given:
        json '''
            [
                "foo",
                2,
                true,
                false,
                null
            ]
        '''

        expect:
        allPointers == [
            '',
            '/',
            '/0',
            '/1',
            '/2',
            '/3',
            '/4',
            ''
        ]
    }

    def "pointers are returned as expected when parsing nested json objects"() {
        given:
        json '''
            {
                "parent": {
                    "child": {
                        "notAnObject": "foo"
                    }
                }
            }
        '''

        expect:
        allPointers == [
            '',
            '/',
            '/',
            '/parent',
            '/parent',
            '/parent/child',
            '/parent/child',
            '/parent/child/notAnObject',
            '/parent',
            '/',
            ''
        ]
    }

    def "pointers are returned as expected when parsing nested json arrays"() {
        given:
        json '''
            [
                [
                    [],
                    "foo"
                ]
            ]
        '''

        expect:
        allPointers == [
            '',
            '/',
            '/0',
            '/0/0',
            '/0',
            '/0/1',
            '/',
            ''
        ]
    }

    def "pointers are returned as expected when parsing arrays nested in json objects"() {
        given:
        json '''
            {
                "topLevel": ["value"],
                "parent": {
                    "child": [2]
                }
            }
        '''

        expect:
        allPointers == [
            '',
            '/',
            '/',
            '/topLevel',
            '/topLevel/0',
            '/',
            '/',
            '/parent',
            '/parent',
            '/parent/child',
            '/parent/child/0',
            '/parent',
            '/',
            ''
        ]
    }

    def "pointers are returned as expected when parsing objects nested in json arrays"() {
        given:
        json '''
            [
                {
                    "topLevelObjectKey": "value"
                },
                [
                    {
                        "nestedObjectKey": 2
                    }
                ]
            ]
        '''

        expect:
        allPointers == [
            '',
            '/',
            '/0',
            '/0',
            '/0/topLevelObjectKey',
            '/',
            '/1',
            '/1/0',
            '/1/0',
            '/1/0/nestedObjectKey',
            '/1',
            '/',
            ''
        ]
    }

    def "pointers are returned as expected when skipping objects"() {
        given:
        json '''
            {
                "skippedObjectKey": {
                    "skippedPropertyKey": false
                },
                "notSkippedValueKey": 123
            }
        '''

        when:
        3.times { parser.next() }

        then:
        parser.pointer == "/skippedObjectKey"

        when:
        parser.skipObject()

        then:
        allPointers == [
            '/',
            '/',
            '/notSkippedValueKey',
            ''
        ]
    }

    def "pointers are returned as expected when skipping arrays"() {
        given:
        json '''
            [
                [1, 2, 3],
                [1]
            ]
        '''

        when:
        2.times { parser.next() }

        then:
        parser.pointer == "/0"

        when:
        parser.skipArray()

        then:
        allPointers == [
            '/',
            '/1',
            '/1/0',
            '/',
            ''
        ]
    }


    void json(String json) {
        def jsonReader = new StringReader(json)
        def backingParser = JsonProvider.provider().createParser(jsonReader)
        parser = new JsonPointerJsonParser(backingParser)
    }

    List<String> getAllPointers() {
        def pointers = [parser.pointer]
        while(parser.hasNext()) {
            parser.next()
            pointers << parser.pointer
        }
        pointers
    }

}
