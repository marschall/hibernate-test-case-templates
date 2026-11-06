package org.hibernate.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.hibernate.StatelessSession;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchImpl;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.batch.spi.GroupedBatch;
import org.hibernate.engine.jdbc.batch.spi.SingleStatementBatch;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * Although ORMStandaloneTestCase is perfectly acceptable as a reproducer, usage of this class is much preferred.
 * Since we nearly always include a regression test with bug fixes, providing your reproducer using this method
 * simplifies the process.
 * <p>
 * What's even better?  Fork hibernate-orm itself, add your test case directly to a module's unit tests, then
 * submit it as a PR!
 */
@DomainModel(
		annotatedClasses = SampleEntity.class
		)
@ServiceRegistry(
		settings = {
				@Setting(name = "hibernate.jdbc.batch_size", value = "2"),
				@Setting(name = AvailableSettings.BUILDER, value = "org.hibernate.bugs.ORMUnitTestCase$CountingBatchBuilderImpl")
		}
		)
@SessionFactory
class ORMUnitTestCase {

	private static final ThreadLocal<Integer> IMPLICIT_FLUSHES = new ThreadLocal<>();

	private static final ThreadLocal<Integer> EXPLICIT_FLUSHES = new ThreadLocal<>();

	@BeforeEach
	void resetFlushes() {
		IMPLICIT_FLUSHES.set( 0 );
		EXPLICIT_FLUSHES.set( 0 );
	}

	@Test
	void insertMultiple(SessionFactoryScope scope) throws Exception {
		scope.inStatelessTransaction( statelessSession -> {
			List<SampleEntity> entities = generateEntities(10);
			statelessSession.insertMultiple(entities);
		} );
		assertBatchingActive();
		assertNoImplicitFlushes();
	}

	@Test
	void insertMultiple_statelessWithOptions(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(session -> {
			try (StatelessSession statelessSession = session.statelessWithOptions().connection().open()) {
				List<SampleEntity> entities = generateEntities(10);
				statelessSession.insertMultiple(entities);
			}
		});
		assertBatchingActive();
		assertNoImplicitFlushes();
	}

	private static List<SampleEntity> generateEntities(int count) {
		return IntStream.range(0, count)
				.mapToObj(i -> new SampleEntity())
				.toList();
	}

	private static void assertBatchingActive() {
		// one for the sequence, one for the insert
		assertEquals(2, EXPLICIT_FLUSHES.get(), "explicit flushes");
	}

	private void assertNoImplicitFlushes() {
		assertEquals(0, IMPLICIT_FLUSHES.get(), "implicit flushed");
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
		IMPLICIT_FLUSHES.remove();
		EXPLICIT_FLUSHES.remove();
	}

	static class CountingBatchObserver implements BatchObserver {

		@Override
		public void batchExplicitlyExecuted() {
			EXPLICIT_FLUSHES.set( EXPLICIT_FLUSHES.get() + 1 );
		}

		@Override
		public void batchImplicitlyExecuted() {
			IMPLICIT_FLUSHES.set(  IMPLICIT_FLUSHES.get() + 1 );
		}

	}

	public static class CountingBatchBuilderImpl extends BatchBuilderImpl {

		public CountingBatchBuilderImpl() {
			super(2);
		}

		@Override
		public BatchImpl buildBatch(BatchKey batchKey, Integer sizeOverride, String table, SessionImplementor session,
				String sql) {
			BatchImpl batch = super.buildBatch(batchKey, sizeOverride, table, session, sql);
			batch.addObserver(new CountingBatchObserver());
			return batch;
		}

		@Override
		public GroupedBatch buildGroupedBatch(BatchKey key, Integer explicitBatchSize,
				Supplier<PreparedStatementGroup> statementGroupSupplier, JdbcCoordinator jdbcCoordinator) {
			GroupedBatch batch = super.buildGroupedBatch(key, explicitBatchSize, statementGroupSupplier, jdbcCoordinator);
			batch.addObserver(new CountingBatchObserver());
			return batch;
		}

		@Override
		public SingleStatementBatch buildSingleStatementBatch(BatchKey key, Integer explicitBatchSize,
				PreparableMutationOperation mutationOperation, JdbcCoordinator jdbcCoordinator) {
			SingleStatementBatch batch = super.buildSingleStatementBatch(key, explicitBatchSize, mutationOperation, jdbcCoordinator);
			batch.addObserver(new CountingBatchObserver());
			return batch;
		}

	}

}

