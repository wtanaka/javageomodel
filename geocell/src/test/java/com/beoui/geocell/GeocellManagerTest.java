package com.beoui.geocell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.beoui.geocell.model.BoundingBox;
import com.beoui.geocell.model.GeocellQuery;
import com.beoui.geocell.model.Point;
import com.beoui.utils.JPAEntity;
import com.beoui.utils.ObjectToSave;

@RunWith(MockitoJUnitRunner.class)
public class GeocellManagerTest {

	private Point center;
	@Mock private PersistenceManager persistenceManager;
	@Mock private EntityManager entityManager;
	@Mock private Query jdoQuery;
	@Mock private javax.persistence.Query jpaQuery;

	@Before
	public void setUp() throws Exception {
		center = new Point(0.0, 0.0);
	}

	@Test
	public void testProximityFetchWithJDO() {
		GeocellQuery baseQuery = new GeocellQuery();
		List<ObjectToSave> queryResults = new ArrayList<ObjectToSave>();
		when(persistenceManager.newQuery(ObjectToSave.class, " geocells.contains(geocellsP)")).thenReturn(jdoQuery);
		when(jdoQuery.execute(any())).thenReturn(queryResults);

		List<ObjectToSave> results = GeocellManager.proximitySearch(center, 1, 10.0, ObjectToSave.class, baseQuery, persistenceManager, 1);

		verify(jdoQuery).declareParameters("String geocellsP");
		assertNotNull(results);
        assertTrue(results.isEmpty());
	}

	@Test
	public void testProximityFetchWithJDOForComplexQuery() {
		List<Object> parameters = new ArrayList<Object>();
		parameters.add("test");
		GeocellQuery baseQuery = new GeocellQuery("baseQuery", "declaredParameters", parameters );
		List<ObjectToSave> queryResults = new ArrayList<ObjectToSave>();
		when(persistenceManager.newQuery(ObjectToSave.class, "baseQuery && geocells.contains(geocellsP)")).thenReturn(jdoQuery);
		when(jdoQuery.executeWithArray(anyVararg())).thenReturn(queryResults);

		List<ObjectToSave> results = GeocellManager.proximitySearch(center, 1, 10.0, ObjectToSave.class, baseQuery, persistenceManager, 1);

		verify(jdoQuery).declareParameters("declaredParameters, String geocellsP");
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

    @Test
    public void testProximityFetchWithJDOForComplexQueryWithOneResult() {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add("test");
        GeocellQuery baseQuery = new GeocellQuery("baseQuery", "declaredParameters", parameters );

        List<ObjectToSave> queryResults = new ArrayList<ObjectToSave>();
        ObjectToSave result1 = new ObjectToSave();
        queryResults.add(result1);
        
        when(persistenceManager.newQuery(ObjectToSave.class, "baseQuery && geocells.contains(geocellsP)")).thenReturn(jdoQuery);
        when(jdoQuery.executeWithArray(anyVararg())).thenReturn(queryResults);

        List<ObjectToSave> results = GeocellManager.proximitySearch(center, 1, 10.0, ObjectToSave.class, baseQuery, persistenceManager, 1);

        verify(jdoQuery).declareParameters("declaredParameters, String geocellsP");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(result1, results.get(0));
    }

    @Test
    public void testProximityFetchWithJDOForComplexQueryWithMultipleResult() {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add("test");
        GeocellQuery baseQuery = new GeocellQuery("baseQuery", "declaredParameters", parameters );

        List<ObjectToSave> queryResults1 = new ArrayList<ObjectToSave>();
        ObjectToSave result1 = new ObjectToSave();
        result1.setId(1L);
        queryResults1.add(result1);

        List<ObjectToSave> queryResults2 = new ArrayList<ObjectToSave>();
        ObjectToSave result2 = new ObjectToSave();
        result2.setId(2L);
        queryResults2.add(result2);

        when(persistenceManager.newQuery(ObjectToSave.class, "baseQuery && geocells.contains(geocellsP)")).thenReturn(jdoQuery);
        when(jdoQuery.executeWithArray(anyVararg())).thenReturn(queryResults1).thenReturn(queryResults2);

        List<ObjectToSave> results = GeocellManager.proximitySearch(center, 2, 10.0, ObjectToSave.class, baseQuery, persistenceManager, 1);

        verify(jdoQuery, times(2)).declareParameters("declaredParameters, String geocellsP");
        assertNotNull(results);
        assertTrue(results.size() > 1);
    }

	@Test
	public void testProximityFetchWithJPASimpleQuery() {
		GeocellQuery baseQuery = new GeocellQuery("SELECT e FROM JPAEntity");

		when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
		when(jpaQuery.getResultList()).thenReturn(new ArrayList<ObjectToSave>());

		List<JPAEntity> results = GeocellManager.proximitySearch(center, 10, 10.0, JPAEntity.class, baseQuery, entityManager, 1);

		assertNotNull(results);
		verify(entityManager).createQuery("SELECT e FROM JPAEntity where geoCellsData in ('c')");
	}

	@Test
	public void testProximityFetchWithJPAComplexQuery() {
		List<Object> parameters = new ArrayList<Object>();
		parameters.add("testKeyString");

		GeocellQuery baseQuery = new GeocellQuery("SELECT e FROM JPAEntity WHERE e.keyString = ?1", parameters);

		when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
		when(jpaQuery.getResultList()).thenReturn(new ArrayList<ObjectToSave>());

		List<JPAEntity> results = GeocellManager.proximitySearch(center, 10, 10.0, JPAEntity.class, baseQuery, entityManager, 1);

		assertNotNull(results);
		verify(entityManager).createQuery("SELECT e FROM JPAEntity where e.keyString = ?1 and geoCellsData in ('c')");
		verify(jpaQuery).setParameter(1, "testKeyString");
	}

	@Test
	public void testProximityFetchWithJPAOrderBy() {
		List<Object> parameters = new ArrayList<Object>();
		parameters.add("testKeyString");

		GeocellQuery baseQuery = new GeocellQuery("SELECT e FROM JPAEntity WHERE e.keyString = ?1 ORDER BY e.keyString", parameters);

		when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
		when(jpaQuery.getResultList()).thenReturn(new ArrayList<ObjectToSave>());

		List<JPAEntity> results = GeocellManager.proximitySearch(center, 10, 10.0, JPAEntity.class, baseQuery, entityManager, 1);

		assertNotNull(results);
		verify(entityManager).createQuery("SELECT e FROM JPAEntity where e.keyString = ?1 and geoCellsData in ('c') order by e.keyString");
		verify(jpaQuery).setParameter(1, "testKeyString");
	}

	@Test
	public void testProximityFetchWithJPAOrderByCapitalised() {
		List<Object> parameters = new ArrayList<Object>();
		parameters.add("testKeyString");

		GeocellQuery baseQuery = new GeocellQuery("SELECT e FROM JPAEntity WHERE e.keyString = ?1 ORDER  BY e.keyString", parameters);

		when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
		when(jpaQuery.getResultList()).thenReturn(new ArrayList<ObjectToSave>());

		List<JPAEntity> results = GeocellManager.proximitySearch(center, 10, 10.0, JPAEntity.class, baseQuery, entityManager, 1);

		assertNotNull(results);
		verify(entityManager).createQuery("SELECT e FROM JPAEntity where e.keyString = ?1 and geoCellsData in ('c') order by e.keyString");
		verify(jpaQuery).setParameter(1, "testKeyString");
	}

	@Test
    public void testProximityFetchWithJPAWithMultipleResult() {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add("testKeyString");
        GeocellQuery baseQuery = new GeocellQuery("SELECT e FROM JPAEntity WHERE e.keyString = ?1", parameters);

        List<ObjectToSave> queryResults1 = new ArrayList<ObjectToSave>();
        ObjectToSave result1 = new ObjectToSave();
        result1.setId(1L);
        result1.setLatitude(2.0);
        result1.setLongitude(-2.0);
        queryResults1.add(result1);
        ObjectToSave result3 = new ObjectToSave();
        result3.setId(3L);
        result3.setLatitude(2.0);
        result3.setLongitude(2.0);
        queryResults1.add(result3);

        List<ObjectToSave> queryResults2 = new ArrayList<ObjectToSave>();
        ObjectToSave result2 = new ObjectToSave();
        result2.setId(2L);
        result2.setLatitude(-2.0);
        result2.setLongitude(-2.0);
        queryResults2.add(result2);
        ObjectToSave result4 = new ObjectToSave();
        result4.setId(4L);
        result4.setLatitude(-2.0);
        result4.setLongitude(2.0);
        queryResults2.add(result4);

		when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
        when(jpaQuery.getResultList()).thenReturn(queryResults1).thenReturn(queryResults2);

        List<JPAEntity> results = GeocellManager.proximitySearch(center, 5, 10.0, JPAEntity.class, baseQuery, entityManager, 1);

		assertNotNull(results);
		verify(entityManager).createQuery("SELECT e FROM JPAEntity where e.keyString = ?1 and geoCellsData in ('c')");
		verify(entityManager).createQuery("SELECT e FROM JPAEntity where e.keyString = ?1 and geoCellsData in ('c')");
		verify(entityManager).createQuery("SELECT e FROM JPAEntity where e.keyString = ?1 and geoCellsData in ('3','9')");
		verify(jpaQuery, times(3)).setParameter(1, "testKeyString");
    }
	
	@Test
	public void testBestBoxSearchOnAntimeridian() {
		float east = 64.576263f;
		float west = 87.076263f;
		float north = 76.043611f;
		float south = -54.505934f;
		Set<String> antimeridianSearch = new HashSet<String>(GeocellManager.bestBboxSearchCells(new BoundingBox(north,east,south,west), null));
		
		List<String> equivalentSearchPart1 = GeocellManager.bestBboxSearchCells(new BoundingBox(north,east,south,-180.0f), null);
		List<String> equivalentSearchPart2 = GeocellManager.bestBboxSearchCells(new BoundingBox(north,180.0f,south,west), null);
		Set<String> equivalentSearch = new HashSet<String>();
		equivalentSearch.addAll(equivalentSearchPart1);
		equivalentSearch.addAll(equivalentSearchPart2);
		
		assertEquals(equivalentSearch, antimeridianSearch);
	}

}
