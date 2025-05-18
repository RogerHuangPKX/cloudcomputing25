package com.archemy.searchapp.model.entities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.jbo.AttributeDef;
import oracle.jbo.server.EntityDefImpl;
import oracle.jbo.server.EntityImpl;
import oracle.jbo.server.TransactionEvent;


public class AutoIncrementEntityImpl extends EntityImpl {
  public AutoIncrementEntityImpl() {
    super();
  }

  protected void doDML(int i, TransactionEvent transactionEvent) {
    super.doDML(i, transactionEvent);
    if (i == DML_INSERT) {
      populateAutoincrementAtt();
    }
  }

  private void populateAutoincrementAtt() {
    EntityDefImpl entdef = this.getEntityDef();
    AttributeDef pk = null;
    //look for primary key with Autoincrement property set
    for (AttributeDef att : entdef.getAttributeDefs()) {
      if (att.isPrimaryKey() &&
          (att.getProperty("AI") != null && new Boolean(att.getProperty("AI").toString()))) {
        pk = att;
        break;
      }
    }
    if (pk != null) {
      try (PreparedStatement stmt =
           this.getDBTransaction().createPreparedStatement("SELECT last_insert_id()", 1)) {
        stmt.execute();
        try (ResultSet rs = stmt.getResultSet()) {
          if (rs.next()) {
            setAttribute(pk.getName(), rs.getInt(1));
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
