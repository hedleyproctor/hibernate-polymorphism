package com.hedleyproctor;

import static org.testng.Assert.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.hedleyproctor.domain.CampingStove;
import com.hedleyproctor.domain.Chair;
import com.hedleyproctor.domain.Phone;
import com.hedleyproctor.domain.Product;
import com.hedleyproctor.domain.RingProduct;
import com.hedleyproctor.domain.Tent;

public class HibernatePolymorphismTest 
{
	private BasicDataSource dataSource;
	private SessionFactory sessionFactory;
	
	public BasicDataSource getDataSource() {
		return dataSource;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	@BeforeMethod
	public void setUp() throws SQLException {
		// note that this datasource and hibernate session are actually separate
		// The Hibernate session is making its own connections to the same db, not using
		// this datasource. This is because if you are using Hibernate without spring,
		// it isn't easy to pass in a datasource to use, whereas it is trivially easy
		// in spring. However I have defined this datasource so you can prove to yourself
		// that changes made by Hibernate really have been flushed to the db.
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
        dataSource.setUrl("jdbc:hsqldb:mem:HibernateDB");
        dataSource.setUsername("sa");
        dataSource.setDefaultAutoCommit(false);
        dataSource.setPassword("");
        dataSource.setAccessToUnderlyingConnectionAllowed(true);
		// note that as of Hibernate 4, the preferred mechanism to build a session factory
		// is by providing a ServiceRegistry object, that includes not just details of your
		// configuration, but also all of the other services that Hibernate may use
		sessionFactory = new Configuration().configure().buildSessionFactory();
	}
	
	@AfterMethod
	public void tearDown() throws SQLException {
		dataSource.close();
		dataSource = null;
		sessionFactory.close();
		sessionFactory = null;
	}
	

	@Test
	public void basicSave() throws SQLException {
		Product product = new Product();
    	product.setName("Test product");

    	Session session = getSessionFactory().openSession();
    	Transaction tx = session.beginTransaction();
    	session.save(product);
    	System.out.println("Saved id: " + product.getId());
    	
    	tx.commit();
    	session.close();
    	
    	Connection connection = getDataSource().getConnection();
    	Statement statement = connection.createStatement();
    	ResultSet resultSet = statement.executeQuery("select count(*) from Product");
    	resultSet.next();
    	// count should be 1
    	assertEquals(resultSet.getLong(1),1);
    	
    	statement = connection.createStatement();
    	resultSet = statement.executeQuery("select id,name from Product");
    	while (resultSet.next()) {
    		System.out.println("Id: " + resultSet.getLong("id") + " Name: " + resultSet.getString("name"));
    	}
    	connection.close();
	}
	
	@Test
	public void implicitPolymorphism() {
		System.out.println("Testing implicit polymorphism");
		RingProduct ringProduct = new RingProduct();
		ringProduct.setName("Diamond ring");
		ringProduct.setStoneSize("0.5ct");
		ringProduct.setStoneType("Diamond");
		
		Session session = getSessionFactory().openSession();
    	Transaction tx = session.beginTransaction();
    	session.save(ringProduct);
    	System.out.println("Saved id: " + ringProduct.getId());

    	// now do a query for all objects of type Product, the superclass
    	List products = session.createQuery("from Product").list();
    	assertEquals(products.size(),1);
    	System.out.println("Found " + products.size() + " products.");
    	RingProduct product = (RingProduct) products.iterator().next();
    	System.out.println("Got product with name: " + product.getName() + " stone size: " + product.getStoneSize() + " and stone type: " + product.getStoneType());
    	
    	tx.commit();
    	session.close();
	}
	
	/** Abstract class at the top of the hierarchy does not correspond to a db table,
	 * but each concrete class gets its own table. This means you only have to load from
	 * a single table when loading a single subtype, but when loading multiple subtypes,
	 * the underlying query will have to use a union. Since there is no connection between
	 * the tables from a database perspective, there is no way to use a join to combine the
	 * results at the database level.
	 * 
	 */
	@Test
	public void tablePerConcreteSubclass() {
		System.out.println("Testing table per concrete subclass");
		Chair chair = new Chair();
		chair.setName("Cross back oak dining chair");
		chair.setDescription("A traditional dining chair with carved feet");
		chair.setMaterial("Velvet");
		
		Session session = getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		session.save(chair);
		
		List furnitureProducts = session.createQuery("from FurnitureProduct").list();
		assertEquals(furnitureProducts.size(),1);
		
		tx.commit();
		session.close();
	}
	
	/** Table per class or joined subclass. Properties in the superclass are stored in the superclass table
	 * so in general a query has to perform a join to retrieve the superclass and subclass properties.
	 * @throws SQLException 
	 * 
	 */
	@Test
	public void tablePerClass() throws SQLException {
		System.out.println("Testing table per class");
		Phone phone = new Phone();
		phone.setName("Samsung S4");
		phone.setDescription("Latest incarnation of Samsung's top of the range smartphone.");
		phone.setScreenSize("4.2 inches");
		phone.setStorage("16Gb");
		
		Session session = getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		session.save(phone);
		
		// do a polymorphic query for the top level electrical product type
		List electricalProducts = session.createQuery("from ElectricalProduct").list();
		assertEquals(electricalProducts.size(),1);
		
		tx.commit();
		session.close();
	
		// prove to ourselves that we really have two tables in the db
	 	Connection connection = getDataSource().getConnection();
    	Statement statement = connection.createStatement();
    	ResultSet resultSet = statement.executeQuery("select count(*) from ElectricalProduct");
    	resultSet.next();
    	// count should be 1
    	assertEquals(resultSet.getLong(1),1);
    	resultSet = statement.executeQuery("select count(*) from phone");
    	resultSet.next();
    	assertEquals(resultSet.getLong(1),1);
    	connection.close();
	}
	
	/** Single table for entire class hierarchy. Classes are distinguished by use of a discriminator column.
	 *  Highly performant, as no joins required when doing queries.
	 * However, not good from a database normalization point of view as all subclass property columns must be
	 * nullable, since any given row could represent any of the subclasses.
	 * @throws SQLException 
	 * 
	 */
	@Test
	public void tablePerClassHierarchy() throws SQLException {
		CampingStove campingStove = new CampingStove();
		campingStove.setName("Firefly");
		campingStove.setDescription("Lightweight stove suitable for backpackers and wilderness campers");
		campingStove.setFuelType("White spirit");
		
		Tent tent = new Tent();
		tent.setName("Mistral");
		tent.setDescription("Lightweight three pole geodesic tent");
		tent.setWeight(3.1);
		tent.setCapacity(2);
		
		Session session = sessionFactory.openSession();
		Transaction tx = session.beginTransaction();
		session.save(campingStove);
		session.save(tent);
		
		// polymorphic query
		List campingProducts = session.createQuery("from CampingProduct").list();
		assertEquals(campingProducts.size(),2);
		
		tx.commit();
		session.close();

	 	Connection connection = getDataSource().getConnection();
    	Statement statement = connection.createStatement();
    	ResultSet resultSet = statement.executeQuery("select count(*) from CampingProduct");
    	resultSet.next();
    	// count should be 1
    	assertEquals(resultSet.getLong(1),2);
    	
    	// show what we stored
    	resultSet = statement.executeQuery("select type,name from campingproduct");
    	while (resultSet.next()) {
    		System.out.println("Product type: " + resultSet.getString("type") + " Name: " + resultSet.getString("name"));
    	}
    	connection.close();
		
	}
}
