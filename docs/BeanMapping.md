# TL;DR
In this framework, objects are a representation of a tuple in the Java world, the only thing you need to take care of is how to map columns to fields. Conversion between snakeCase for the Java beans and camel_case for the database is by default and foreign keys adds implicitly \_id at the end. Custom behaviour is customized with @Column. Accessing fields is made using the standard ways.

# Naming conventions
The naming convention is the default behaviour of translating a bean property name in a column name. This behaviour is driven by an implementation of the interface [NamingConvention](../core/src/main/java/fr/petitl/relational/repository/template/bean/NamingConvention.java).                                                     

## Default behaviour
In relational database the case-sensitivity is often problematic. This is often due to the casing support in file systems. That is why the most common column names in databases are camel cased. Also, JDBC column names case in ResultSetMetaData are often forced. Therefore, by default, all fields are named using their camel cased form for their column name.

The default behaviour is available in [CamelToSnakeConvention](../core/src/main/java/fr/petitl/relational/repository/template/bean/CamelToSnakeConvention.java).

Some examples:

| Field Name   | Column Name    |
| ------------ |----------------|
| createdBy    | created_by     |
| addressLine2 | address_line_2 |
| someFKId     | some_fk_id     |
| created_by   | created_by     |

As you can see, the conversion is done as close as possible to the human interpretation.

### Foreign Keys
A foreign key field is a field that leads to an other table primary key. Therefore, in classical database design, a postfix ```_id``` is often used. To do this the most efficient way without hurting too much the field naming, this postfix is automatically added if the field is detected as a foreign key handle (see [the foreign key documentation](ForeignKey)) AND if the generated column name does not already have the postfix at the end.

This way you would have the following mapping:
```java
class A {
    // Maps to id
    int id;
    
    // Maps to first_name
    String firstName;
    
    // Maps to account_id
    FK<Integer, Account> account;
}
```

## Name override
Any column name can be overridden with the annotation ```@Column```. This will override all other behaviour from there (like postfix shenanigans). 
```java
    @Column(name = "lastname")
    String lastName;
```

## Naming Convention override
The naming convention can be changed in the constructor of the relational template. If you need, change the implementation that fits your needs. But please note that you can configure the default behaviour by creating the instance of the CamelToSnakeConvention with a custom postfix (or no postfix if desired).

# Field selection
All declared fields in a class are by default mapped to a column, at the exception of static and transient modifiers.

# Accessors
In order to read and write into fields, the framework detects the best way to set/get values. For each field, two functions are created to take care of this. The following rules applies:
* If a [property descriptor](http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/BeanUtils.html#getPropertyDescriptor-java.lang.Class-java.lang.String-) is available, then use read and write methods (e.g. declared getters and setters)
* If not, then ```Class.get``` and ```Class.set``` are used and if the field is not public it will try to override permissions. In case of security exception the mapping crashes.

# Mappers
Each field has accessors but also mappers in order to read and write its value from result sets and into statements. By default the mappers are defined in the [dialect](Dialect.md) and are detailed in the [data mapping section](DataMapping.md). Those mappers are the [BeanAttributeReader](../core/src/main/java/fr/petitl/relational/repository/template/bean/BeanAttributeReader.java). and [BeanAttributeWriter](../core/src/main/java/fr/petitl/relational/repository/template/bean/BeanAttributeWriter.java). 

Aside from per field mapping, each class has a [BeanMapper](../core/src/main/java/fr/petitl/relational/repository/template/bean/BeanMapper.java) which maps an entire tuple (taken from a result set) to a new instance of a class.

_Important: an empty public constructor is mandatory to create objects._
 
Conversely, a [BeanUnmapper](../core/src/main/java/fr/petitl/relational/repository/template/bean/BeanUnmapper.java) sets  statement arguments with the values from a bean. This supports an offset (useful when using multiple rows in an insert for instance) and the order is respected based on the given field data. 

The Bean-level mappers can not be overriden.

# Accessing internals
In the template it is possible to call ```getMappingData``` which will return a [BeanMappingData](../core/src/main/java/fr/petitl/relational/repository/template/bean/BeanMappingData.java) which contains everything that is necessary to map objects.
* The list of fields meta data which contains
    * The column name
    * Getter / Setter
    * JDBC Reader / Writer
    * The reflect field
* The bean mapper
* The insert into unmapper which respects the list of field order
* An index to get a field meta data based on a column name
* The reflect class

This metadata is cached at the template level.