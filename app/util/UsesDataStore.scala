package util

import java.util.Properties
import javax.jdo.JDOHelper
import scalajdo.DataStore
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory

object UsesDataStore {
  val dataStore: DataStore = new DataStore(() => {
    val props = new Properties()
    props.put("datanucleus.PersistenceUnitName", "webscala")
    JDOHelper.getPersistenceManagerFactory(props).asInstanceOf[JDOPersistenceManagerFactory]
  })
}

trait UsesDataStore {
  def dataStore = UsesDataStore.dataStore
}
