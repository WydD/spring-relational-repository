# spring-relational-repository
Light and straight to the point implementation of Spring Repositories using JDBC without JPA or any high-level entity manager

Warning: this is still in WIP!

## Concepts
Some insights about why this framework exists. JPA's implementations (like hibernate) takes care of a lot of things,
sometimes things that should be handled by the developer. The problem arise when the developer has no idea what is the 
physical design of the database, or if he his inside a transaction or not.
 
The idea behind this framework is to have an implementation of the Spring Data repositories which is as close to the JDBC layer
 as possible and also to be as explicit as possible.

Here is what you will NOT have:
* An Entity Manager
* Query caching (on any level)
* Schema generation
* Implicit entity resolution in foreign key management
* JPQL implementation

Here is what you WILL have:
* Spring Data repositories
* Entity mapping
* Query specific object mappings
* Custom mappers on each fields
* Foreign key resolution tools
* Dialect handling
* Query execution helpers

Here is what you can use with it:
* The spring transaction manager
* Connection and resource pooling (like DBCP or C3P0)

Additionally, this implementation is only available for Java 8. Which makes it more powerful as it can use the Stream 
implementation and lambdas awesomeness which makes the data management way easier now.

## Simple stuff

### Configuration
Spring Relation Repository uses the straight forward programmatic configuration of spring.
```java
@Configuration
@EnableRelationalRepositories(basePackages = "foo.bar.repositories")
public class Configuration {
    @Bean
    public DataSource dataSource() {
        return /* your configured source */ new DataSourceImpl(); 
    }

    @Bean
    public RelationalTemplate relationalTemplate(DataSource dataSource) {
        BeanDialect dialect = GenericDialectProvider.mysql();
        return new RelationalTemplate(dataSource, dialect);
    }
}
```

The annotation EnableRelationalRepositories is a standard Spring Data repository scan. Check out their documentation for more information on this matter.

The template creation however is specific. It takes two arguments, firstly the JDBC data source, then the dialect. This object
specifies how to generate the necessary SQL for your implementation and the default bean mappers.

### Domain
Your domain must have the following shape 

```java
@Table("Pojo")
public class Pojo {
    @PK
    private String id;

    private String name;

    // This is mapped to the column `created_date`
    private Date createdDate;

    public Pojo() {
    }                 
    
    /* ... getters and setters ... */
}
```

The annotation ```@Table``` specifies which table it must map the domain type to. ```@PK``` specifies the primary key.
 The rest of the fields are assumed to be mapped automatically. If necessary a ```@Column``` annotation can be used to
 specify mappings and names.

### Repositories
Repositories are handled like any spring data repositories. Use the RelationalRepository interface to have all features.

```java
public interface PojoRepository extends RelationalRepository<Pojo, String> {
    @Query("SELECT * FROM Pojo WHERE name = :name")
    Pojo findByName(@Param("name") String name);
}
```

The annotation ```@Query``` specifies the query in the native SQL supported by the database. Positional and named parameters are
supported as in usual JPQL queries.

Note: The annotation is mandatory! No implementation has been made to generate methods based on its name (not explicit enough and can be confusing as few SQL generation is done in this framework).

### Java 8 Stream specifics
Repositories are technically able to produce a Java 8 Stream in a ```findAll``` method for instance. However, it has been chosen
 to not provide such feature.

Let's take this simple example:
```java
// Get the stream of Pojos
Stream<Pojo> s = repository.streamAll();
// Create a index name -> our objects
Map<String, Pojo> pojoIndex = s.collect(Collectors.toMap(Pojo::getName, p -> p));
```

This example is wrong as the resources are either closed during streamAll or are still opened
 after the stream consumption. In Java 8, no resource handling can be done on streams without explicit declaration.

A good way of handling this is:
```java                                 
Map<String, Pojo> pojoIndex;
try (Stream<Pojo> s = repository.streamAll()) {
    // Create a index name -> our objects
    pojoIndex = s.collect(Collectors.toMap(Pojo::getName, p -> p));
}
```

The Java 7 syntax of try-with-resource will effectively closes its resources after the consumption even if their is an exception.
However, this is the developer responsibility to correctly implement this. 

To avoid errors and to make everything more explicit, here's what has been chosen:
```java
Map<String, Pojo> pojoIndex = repository.streamAll(stream -> stream.collect(Collectors.toMap(Pojo::getName, p -> p)));
```

The stream implementations takes as argument a function that maps a Stream of objects to a templated result.
 If the function returns a unconsumed stream, its resources are freed so you will not be able to make anything of it.

You can use this kind of syntax in your generated repository function using the ```@CollectorFunction``` annotation.
```java
@Query("SELECT * FROM Pojo WHERE name = ?0")
<E> E findByNameStream(String name, @CollectorFunction(Pojo.class) Function<Stream<Pojo>, E> apply);
```

TODO: GroupBy helper in queries

### Object Mappings
By default, association between fields and column is done via their names. *It is assumed that Java beans uses
 the camel case convention and Database tables uses the snake case convention*. 

The default type mapping is completely done using Spring JDBC Utils: StatementCreatorUtils.javaTypeToSqlParameterType for writing
 and JdbcUtils.getResultSetValue. Those method calls can be found and changed in the dialect.

Repositories are able to map objects to their tables. But queries can map their row to data transfer objects like:
```java
@Query("SELECT name, count(*) as `count` FROM Pojo GROUP BY name")
List<PojoCount> countByName();
```

```PojoCount``` being a class with two fields, String name and int count. ```@Column``` annotations are used for its mapping.
 
Note that it can be more interesting to have something that resembles this: 
```java
@Query("SELECT name, count(*) as `count` FROM Pojo GROUP BY name")
<E> E countByName(@CollectorFunction(PojoCount.class) Function<Stream<PojoCount>, E> apply);
```

Therefore, you can call this method to directly create a Map using something like this:
```java
Map<String, Integer> result = repository.countByName(s -> s.collect(Collectors.toMap(PojoCount::getName, PojoCount::getCount));
```

You are therefore assured that no intermediate structure have been created before your final result.

#### Advanced: custom mappers
Mapping can be customized using the ```@Column``` annotation. Obviously, you can change the name, but you can also change read-write operations.
 For instance if you want to map a complex object to a ```jsonb``` column in Postgres 9.4, here's how to do it.

Declare your custom mapper (called here JsonBMapper) which is able to read and write an attributes.
```java
public class JsonBMapper implements BeanAttributeMapper {
    // Jackson Mapper
    private static ObjectMapper om = new ObjectMapper();

    @Override
    public Object readAttribute(ResultSet rs, int column, Field sourceField) throws SQLException {
        // You have to extract the object in the right type from the JDBC result set
        // In this case, convert the JSON string to the desired type 
        JavaType targetType = om.getTypeFactory().constructType(targetField.getGenericType());
        try {
            return om.readValue(rs.getString(column), targetType);
        } catch (IOException e) {    
            // standard exception handling
            throw new SQLException(e);
        }
    }

    @Override
    public void writeAttribute(PreparedStatement ps, int column, Object o, Field sourceField) throws SQLException {     
        // You have to put the correct object in the prepared statement
        // In this case, convert the object into a JSON string that you encapsulate in a PGobject
        PGobject pg = new PGobject();
        pg.setType("jsonb");
        try {
            // Pure JSON serialization
            pg.setValue(om.writeValueAsString(o));
            ps.setObject(column, pg);
        } catch (JsonProcessingException e) {
            // standard exception handling
            throw new SQLException(e);
        }
    }
}
```

Now you can use your mapper in your domain objects or in your data transfer objects.
```java
@Column(mapper = JsonBMapper.class)
Map<String, Integer> keywords;
```