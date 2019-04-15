/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.microsoft.rest.serializer.JacksonAdapter;
import com.microsoft.rest.serializer.JsonFlatten;
import com.microsoft.rest.util.Foo;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlatteningSerializerTests {
    @Test
    public void canFlatten() throws Exception {
        Foo foo = new Foo();
        foo.bar = "hello.world";
        foo.baz = new ArrayList<>();
        foo.baz.add("hello");
        foo.baz.add("hello.world");
        foo.qux = new HashMap<>();
        foo.qux.put("hello", "world");
        foo.qux.put("a.b", "c.d");
        foo.qux.put("bar.a", "ttyy");
        foo.qux.put("bar.b", "uuzz");

        JacksonAdapter adapter = new JacksonAdapter();

        // serialization
        String serialized = adapter.serialize(foo);
        Assert.assertEquals("{\"$type\":\"foo\",\"properties\":{\"bar\":\"hello.world\",\"props\":{\"baz\":[\"hello\",\"hello.world\"],\"q\":{\"qux\":{\"hello\":\"world\",\"a.b\":\"c.d\",\"bar.b\":\"uuzz\",\"bar.a\":\"ttyy\"}}}}}", serialized);

        // deserialization
        Foo deserialized = adapter.deserialize(serialized, Foo.class);
        Assert.assertEquals("hello.world", deserialized.bar);
        Assert.assertArrayEquals(new String[]{"hello", "hello.world"}, deserialized.baz.toArray());
        Assert.assertNotNull(deserialized.qux);
        Assert.assertEquals("world", deserialized.qux.get("hello"));
        Assert.assertEquals("c.d", deserialized.qux.get("a.b"));
        Assert.assertEquals("ttyy", deserialized.qux.get("bar.a"));
        Assert.assertEquals("uuzz", deserialized.qux.get("bar.b"));
    }

    @Test
    public void canSerializeMapKeysWithDotAndSlash() throws Exception {
        String serialized = new JacksonAdapter().serialize(prepareSchoolModel());
        Assert.assertEquals("{\"teacher\":{\"students\":{\"af.B/D\":{},\"af.B/C\":{}}},\"tags\":{\"foo.aa\":\"bar\",\"x.y\":\"zz\"},\"properties\":{\"name\":\"school1\"}}", serialized);
    }

    @Test
    public void canHandleTypeDotDiscriminator() throws IOException {
        List<String> meals = new ArrayList<>();
        meals.add("carrot");
        meals.add("apple");
        //
        FlattenableAnimal animal = new FlattenableRabbit().withMeals(meals);
        JacksonAdapter adapter = new JacksonAdapter();
        String serialized = adapter.serialize(animal);
        //
        String[] results = {
                "{\"meals\":[\"carrot\",\"apple\"],\"@odata.type\":\"#Favourite.Pet.FlattenableRabbit\"}",
                "{\"@odata.type\":\"#Favourite.Pet.FlattenableRabbit\",\"meals\":[\"carrot\",\"apple\"]}"
        };

        boolean found = false;
        for (String result : results) {
            if (result.equals(serialized)) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);
        //
        animal = adapter.deserialize(serialized, FlattenableAnimal.class);
        Assert.assertTrue(animal instanceof FlattenableRabbit);
        FlattenableRabbit rabbit = (FlattenableRabbit) animal;
        Assert.assertNotNull(rabbit.meals());
        Assert.assertEquals(rabbit.meals().size(), 2);
    }

    @Test
    public void Foo2() throws IOException {
        Map<Integer, Integer> d = new HashMap<>();
        d.put(88, 99);
        JacksonAdapter adapter = new JacksonAdapter();
        adapter.serialize(d);
    }

    @Test
    public void canHandlePropertyTypeWithDotDiscriminator() throws IOException {
        //
        List<String> meals = new ArrayList<>();
        meals.add("carrot");
        meals.add("apple");

        FlattenableAnimal animal = new FlattenableRabbit().withMeals(meals);
        FlattenableAnimalInfo transformOutput = new FlattenableAnimalInfo().withAnimal(animal);
        List<FlattenableAnimalInfo> animalsInfo = ImmutableList.of(transformOutput);
        AnimalShelter shelter = new AnimalShelter().withAnimalsInfo(animalsInfo);
        JacksonAdapter adapter = new JacksonAdapter();
        String serialized = adapter.serialize(shelter);
        System.out.println(serialized);
        //
//        JacksonAdapter adapter = new JacksonAdapter();
//        String serialized = adapter.serialize(animal);
//        Assert.assertEquals(serialized, "{\"@odata.type\":\"#Favourite.Pet.FlattenableRabbit\",\"meals\":[\"carrot\",\"apple\"]}");
//        //
//        animal = adapter.deserialize(serialized, FlattenableAnimal.class);
//        Assert.assertTrue(animal instanceof FlattenableRabbit);
//        FlattenableRabbit rabbit = (FlattenableRabbit) animal;
//        Assert.assertNotNull(rabbit.meals());
//        Assert.assertEquals(rabbit.meals().size(), 2);
    }

    @Test
    public void shouldSkipDiscriminator6() throws IOException {
        List<String> codecs = new ArrayList<>();
        codecs.add("A");
        codecs.add("B");
        codecs.add("C");

        FlattenableAnimal preset = new FlattenableRabbit().withMeals(codecs);
        FlattenableAnimalInfo transformOutput = new FlattenableAnimalInfo().withAnimal(preset);
        List<FlattenableAnimalInfo> transformOutputs = ImmutableList.of(transformOutput);
        AnimalShelter inner = new AnimalShelter().withAnimalsInfo(transformOutputs);
        //
        JacksonAdapter adapter = new JacksonAdapter();
        // serialization
        String serialized = adapter.serialize(inner);
        System.out.println(serialized);

        AnimalShelter t = adapter.deserialize(serialized, AnimalShelter.class);
        // Assert.assertTrue(p instanceof FlattenableRabbit);
    }

    @Test
    public void Foo() throws IOException {
        FlattenedProduct p = new FlattenedProduct();
        p.withPname("wwwww");
        p.withFlattenedProductType("chai");
        JacksonAdapter adapter = new JacksonAdapter();
        // serialization
        String serialized = adapter.serialize(p);
        System.out.println(serialized);
    }

    @JsonFlatten
    private class School {
        @JsonProperty(value = "teacher")
        private Teacher teacher;

        @JsonProperty(value = "properties.name")
        private String name;

        @JsonProperty(value = "tags")
        private Map<String, String> tags;

        public School withTeacher(Teacher teacher) {
            this.teacher = teacher;
            return this;
        }

        public School withName(String name) {
            this.name = name;
            return this;
        }

        public School withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }
    }

    private class Student {
    }

    private class Teacher {
        @JsonProperty(value = "students")
        private Map<String, Student> students;

        public Teacher withStudents(Map<String, Student> students) {
            this.students = students;
            return this;
        }
    }

    private School prepareSchoolModel() {
        Teacher teacher = new Teacher();

        Map<String, Student> students = new HashMap<String, Student>();
        students.put("af.B/C", new Student());
        students.put("af.B/D", new Student());

        teacher.withStudents(students);

        School school = new School().withName("school1");
        school.withTeacher(teacher);

        Map<String, String> schoolTags = new HashMap<String, String>();
        schoolTags.put("foo.aa", "bar");
        schoolTags.put("x.y", "zz");

        school.withTags(schoolTags);

        return school;
    }

    @JsonFlatten
    public static class FlattenedProduct {
        /**
         * The pname property.
         */
        @JsonProperty(value = "properties.p\\.name")
        private String pname;

        /**
         * The flattenedProductType property.
         */
        @JsonProperty(value = "properties.type")
        private String flattenedProductType;

        /**
         * Possible values include: 'Succeeded', 'Failed', 'canceled', 'Accepted',
         * 'Creating', 'Created', 'Updating', 'Updated', 'Deleting', 'Deleted',
         * 'OK'.
         */
        @JsonProperty(value = "properties.provisioningStateValues", access = JsonProperty.Access.WRITE_ONLY)
        private String provisioningStateValues;

        /**
         * The provisioningState property.
         */
        @JsonProperty(value = "properties.provisioningState")
        private String provisioningState;

        /**
         * Get the pname value.
         *
         * @return the pname value
         */
        public String pname() {
            return this.pname;
        }

        /**
         * Set the pname value.
         *
         * @param pname the pname value to set
         * @return the FlattenedProduct object itself.
         */
        public FlattenedProduct withPname(String pname) {
            this.pname = pname;
            return this;
        }

        /**
         * Get the flattenedProductType value.
         *
         * @return the flattenedProductType value
         */
        public String flattenedProductType() {
            return this.flattenedProductType;
        }

        /**
         * Set the flattenedProductType value.
         *
         * @param flattenedProductType the flattenedProductType value to set
         * @return the FlattenedProduct object itself.
         */
        public FlattenedProduct withFlattenedProductType(String flattenedProductType) {
            this.flattenedProductType = flattenedProductType;
            return this;
        }

        /**
         * Get possible values include: 'Succeeded', 'Failed', 'canceled', 'Accepted', 'Creating', 'Created', 'Updating', 'Updated', 'Deleting', 'Deleted', 'OK'.
         *
         * @return the provisioningStateValues value
         */
        public String provisioningStateValues() {
            return this.provisioningStateValues;
        }

        /**
         * Get the provisioningState value.
         *
         * @return the provisioningState value
         */
        public String provisioningState() {
            return this.provisioningState;
        }

        /**
         * Set the provisioningState value.
         *
         * @param provisioningState the provisioningState value to set
         * @return the FlattenedProduct object itself.
         */
        public FlattenedProduct withProvisioningState(String provisioningState) {
            this.provisioningState = provisioningState;
            return this;
        }
    }
}
