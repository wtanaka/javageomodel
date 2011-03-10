package com.beoui.geocell;

import static org.junit.Assert.*;

import org.junit.Test;

import com.beoui.geocell.model.Point;
import com.beoui.utils.JPAEntity;

/**
 * Port of http://code.google.com/p/geomodel/source/browse/trunk/geo/geocell_test.py
 * @author edgar.dalmacio@gmail.com
 */
public class GeocellUtilsTest {

	private static final String TEST_KEY_STRING = "ID";
	private Point point = new Point(37, -122);
	
	public static class JPAEntitySubclass extends JPAEntity {
		
	}
	
	@Test
	public void testCompute() {
		// a valid geocell
		String cell = GeocellUtils.compute(point, 14);
		assertEquals(14, cell.length());
		assertTrue(GeocellUtils.isValid(cell));
		assertTrue(GeocellUtils.containsPoint(cell, point));
		
		// a lower resolution cell should be a prefix to a higher resolution
		// cell containing the same point
		String lowresCell = GeocellUtils.compute(point, 8);
	    assertTrue(cell.startsWith(lowresCell));
	    assertTrue(GeocellUtils.containsPoint(lowresCell, point));
	    
	    // an invalid geocell
	    cell = GeocellUtils.compute(new Point(0, 0), 0);
	    assertEquals(0, cell.length());
	    assertFalse(GeocellUtils.isValid(cell));
	}
	
	@Test
	public void testGetKeyString() {
		JPAEntity entity = new JPAEntity();
		entity.setId(TEST_KEY_STRING);

		String keyString = GeocellUtils.getKeyString(entity);

		assertNotNull(keyString);
		assertEquals(TEST_KEY_STRING, keyString);
	}

	@Test
	public void testGetKeyStringWithEntitySubClass() {
		JPAEntitySubclass entity = new JPAEntitySubclass();
		entity.setId(TEST_KEY_STRING);

		String keyString = GeocellUtils.getKeyString(entity);
		
		assertNotNull(keyString);
		assertEquals(TEST_KEY_STRING, keyString);
	}
	
	@Test
	public void testGetLocation() {
		JPAEntity entity = new JPAEntity();
		entity.setLatitude(0.5);
		entity.setLongitude(-0.5);

		Point location = GeocellUtils.getLocation(entity);

		assertNotNull(location);
		assertEquals(0.5, location.getLat(), 0.0);
		assertEquals(-0.5, location.getLon(), 0.0);
	}

	@Test
	public void testGetLocationWithEntitySubClass() {
		JPAEntitySubclass entity = new JPAEntitySubclass();
		entity.setLatitude(0.5);
		entity.setLongitude(-0.5);

		Point location = GeocellUtils.getLocation(entity);

		assertNotNull(location);
		assertEquals(0.5, location.getLat(), 0.0);
		assertEquals(-0.5, location.getLon(), 0.0);
	}
	
	@Test
	public void testGetGeocellsFieldName() {
		String geocellsFieldName = GeocellUtils.getGeocellsFieldName(JPAEntity.class);
		
		assertNotNull(geocellsFieldName);
		assertEquals("geoCellsData", geocellsFieldName);
	}

	@Test
	public void testGetGeocellsFieldNameWithEntitySubClass() {
		String geocellsFieldName = GeocellUtils.getGeocellsFieldName(JPAEntitySubclass.class);
		
		assertNotNull(geocellsFieldName);
		assertEquals("geoCellsData", geocellsFieldName);
	}

	/**
	 * Example calculation taken from https://secure.wikimedia.org/wikipedia/en/wiki/Great-circle_distance#Worked_example
	 */
	@Test
	public void testDistance() {
		Point p1 = new Point(36.12, -86.67);	// Nashville International Airport (BNA) in Nashville, TN, USA
		Point p2 = new Point(33.94, -118.40);	// Los Angeles International Airport (LAX) in Los Angeles, CA, USA

		double distance = GeocellUtils.distance(p1, p2);
		assertEquals(2889677.0, distance, 1.0);
	}
}
