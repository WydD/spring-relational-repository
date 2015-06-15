# TL;DR
Objects are a representation of a tuple in the Java world, the only thing you need to take care of is how to map columns to fields. Conversion between snakeCase for the Java beans and camel_case for the database is by default and foreign keys adds implicitly \_id at the end. Custom behaviour is customized with @Column. Accessing fields is made using the standard ways.

# Naming conventions
The naming convention is the default behaviour of translating a bean property name in a column name. This behaviour is driven by an implementation of the interface ```.template.bean.NamingConvention```.                                                     

## Default behaviour
In relational database the case-sensitivity is often problematic. This is often due to the casing support in file systems. That is why the most common column names in databases are camel cased. Also, JDBC column names case in ResultSetMetaData are often forced. Therefore, by default, all fields are named using their camel cased form for their column name.

The default behaviour is available in ```.template.bean.CamelToSnakeConvention```.

Some examples:

| Field Name | Column Name |
| ---------- |-------------|
| createdBy  | created_by  |
| addressLine2 | address_line_2 |
| someFKId | some_fk_id |
| created_by | created_by |

As you can see, the conversion is done as close as possible to the human interpretation.

### Foreign Keys
A foreign key field is a field that leads to an other table primary key. Therefore, in classical database design, a postfix ```\_id``` is often used. To do this the most efficient way without hurting too much the field naming, this postfix is automatically added if the field is detected as a foreign key handle (see [the foreign key documentation](ForeignKey)) AND if the generated column name does not already have the postfix at the end.

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

# Accessors
