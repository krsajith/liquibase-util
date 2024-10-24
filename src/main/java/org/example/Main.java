package org.example;

import liquibase.change.core.CreateTableChange;
import liquibase.change.core.CreateViewChange;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.ChangeLogParseException;
import liquibase.integration.spring.SpringResourceAccessor;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.parser.core.yaml.YamlChangeLogParser;
import liquibase.precondition.core.PreconditionContainer;
import liquibase.precondition.core.TableExistsPrecondition;
import liquibase.serializer.core.xml.XMLChangeLogSerializer;
import org.springframework.core.io.FileSystemResourceLoader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws ChangeLogParseException, IOException {
        var ymlParser = new YamlChangeLogParser();
        DatabaseChangeLog databaseChangeLog = ymlParser.parse("db.changelog-master.yaml",
                new ChangeLogParameters(), new SpringResourceAccessor(new FileSystemResourceLoader()));

        var children = new ArrayList<ChangeSet>();


        var changeLogSerializer = new XMLChangeLogSerializer();
        for (var changeSet : databaseChangeLog.getChangeSets()) {
            for (var change : changeSet.getChanges()) {
                if (change instanceof CreateTableChange createTableChange) {
                    PreconditionContainer pc = new PreconditionContainer();
                    var condition = new TableExistsPrecondition();
                    condition.setTableName(createTableChange.getTableName());
                    pc.addNestedPrecondition(condition);
                    changeSet.setPreconditions(pc);
                    children.add(changeSet);
                }
                if(change instanceof CreateViewChange createViewChange){
                    var views = new ArrayList<ChangeSet>();
                    views.add(changeSet);
                    changeLogSerializer.write(views, new FileOutputStream(createViewChange.getViewName()+".xml"));
                }
            }
        }


        changeLogSerializer.write(children, new FileOutputStream("tables.xml"));
    }
}