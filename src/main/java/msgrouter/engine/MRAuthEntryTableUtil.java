package msgrouter.engine;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import elastic.util.sqlmgr.SqlConn;
import elastic.util.sqlmgr.dataset.DBRow;

public class MRAuthEntryTableUtil {
	private static final Logger LOG = Logger
			.getLogger(MRAuthEntryTableUtil.class);

	public static boolean existsTable(SqlConn sqlConn) {
		Map pRecord = new DBRow();

		try {
			List<Map> rRecords = sqlConn.queryList("admin.existsAETable",
					pRecord);
		} catch (Exception e) {
			return false;
		} catch (Error e) {
			return false;
		}
		return true;
	}

	public static void deleteTable(SqlConn sqlConn) {
		Map pRecord = new DBRow();

		try {
			sqlConn.queryUpdate("admin.deleteAETable", pRecord, null);
		} catch (Throwable e) {
			LOG.error(e.getMessage());
		}
	}

	public static void createTable(SqlConn sqlConn) {
		Map pRecord = new DBRow();

		try {
			sqlConn.queryUpdate("admin.createAETable", pRecord, null);
		} catch (Throwable e) {
			LOG.error(e.getMessage());
		}
		try {
			sqlConn.queryUpdate("admin.createAETableIdx1", pRecord, null);
		} catch (Throwable e) {
			LOG.error(e.getMessage());
		}
	}
}
