Hibernate Polymorphism Example
==============================

Simple example that demonstrates the four ways of dealing with inheritance and polymorphism in Hibernate:

1. Implicit polymorphism - no explicit mapping of the inheritance, but Hibernate can support polymorphic queries because it understands the class hierarchy.
2. Table per concrete subclass - abstract class at the top of the hierarchy does not correspond to a database table, meaning that any properties in the superclass are duplicated into each subclass table, and polymorphic queries have to use unions.
3. Table per class. All classes, including abstract ones, get a database table, meaning that no fields are duplicated. Better from a normalization point of view, but queries can still be slow because they have to perform joins.
4. Single table for entire class hierarchy. Fast, because no joins or unions required for any queries, but not good from a database normalization perspective, because subclass columns have to be nullable.

The project uses maven, TestNG and HSQLDB. Hence, to run the tests, use:

    mvn test