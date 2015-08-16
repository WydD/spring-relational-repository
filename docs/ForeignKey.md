# TL;DR
As beans are a representation of tuples, the concept of foreign keys is therefore not available in the bean declaration.
 Repositories are able to directly resolve foreign keys via the ```findAll``` and ```resolveFK``` methods.
 For bean mappings with resolution, use the [FKResolver](../core/src/main/java/fr/petitl/relational/repository/repository/FKResolver.java).

To see it in action, read the [FK Tests](../core/src/test/java/fr/petitl/relational/repository/fk).

# Direct resolves
The direct resolve is the operation that transform identifiers (or beans with getters) to their respective bean.

## Single resolve
First and foremost, the simplest of the simplest resolve is actually the ```findOne``` method from any repositories.

Let's say we have table ```Location``` that points to ```Country``` (atomic pk ```id```). Then, the following code
  resolves naturally the country from the location.
```java
Country c = countryRepository.findOne(location.getCountryId())
```

## Multiple resolve
The previous section performs a query. If you need to operate multiple resolve operations, obviously this can be done
 by a single query.

This can be done the old way with the ```findAll``` method from the repositories. This method is not restricted by the
 standard ```findAll(Iterable<ID>)``` from Spring, there is also a version ```findAll(Stream<ID>, CollectorFunction)```.

This works in synergy with the indexed collector function ```asIndexed```. For instance, we could have:
```java
Map<String, Country> countries = countryRepository.findAll(Stream.of("FR", "UK"), countryRepository.asIndexed());
```

You will find in ```countries``` the java Map between the primary key and the java bean.

*!!! :rage1: This requires that the primary key supports hashcode/equals !!!*

Some side note: this will entirely consume the stream (obviously) and this collects in a Set before
  sending to the database.

As a helper, you can provide a stream of a foreign entities with the foreign key getter and get the map back.

```java
Map<String, Country> countries = countryRepository.resolveFK(Stream.of(paris, berlin), Location::getCountryId);
```

If you do not desire the Map but just a list or set or whatever suits your needs, you can provide a collector function
 as always.

```java
List<Country> countries = countryRepository.resolveFK(Stream.of(paris, berlin), Location::getCountryId, st -> st.collect(Collectors.toList()));
```

# Resolution by mapping
This resolution style transforms foreign beans in an other other that has resolved one or multiple foreign keys.

## Concept
The concept is actually pretty straight forward. Let's say we want to translate a ```Location(countryId, id, name)``` into

```java
class LocationDTO {
    Country country;
    String id;
    String name;
}
```

The strategy goes as follows
* Fetch the resolution map for each foreign key to resolve (with the given foreign key getters)
* For each bean
  * Create the new bean with a constructor
  * For each resolver
    * Set the resolved bean on the new one (with the given setter)

## Usage
All this logic has been implemented in the class [FKResolver](../core/src/main/java/fr/petitl/relational/repository/repository/FKResolver.java).

This resolver is built with its own builder. Let's see it in action:

```java
FKResolver<Location, LocationDTO> resolver = FKResolver.of(LocationDTO::new)
    .add(Location::getCountryId, countryRepository, (dto, country) -> dto.country = country)
    .build();

List<LocationDTO> dtos = resolver.resolve(Stream.of(paris, berlin)).collect(Collectors.toList());
```

*Keep in mind that this operation requires to collect all keys before sending it to the database.* This might trigger
 an Out of memory if your input stream is large. That is why, a bulk version has been provided
 ```resolve(Stream<E> stream, int bulkSize)```, this performs the exact same operation but treats it in packs of
 ```bulkSize``` input entities.

Note: This resolver *WORKS* with ```Object[]``` primary keys (a special case has been handled).

## Implementation notes
The builder requires three objects:
* A getter from the input bean type to the primary key type of the target repository
  * The returned value can return null, no resolution is attempted then.
* The target repository
* A setter that takes the target bean type and the resolved object
* An optional boolean that sets if the setter must be called if the resolution is null (due to a key not found or null reference)
  * By default ```setIfNull``` is ```false```.

The resolver applies operations in order as given. Therefore if your setter needs another one to be applied before, then
 place this operation before.

# Other rejected ideas
TODO: Talk about what a ```Reference<ID, T>``` container does not work in the general case.