//
// Groovy Liquibase ChangeLog
//
// Copyright (C) 2010-2011 Tim Berglund
// http://augusttechgroup.com
// Littleton, CO
//
// Licensed under the Apache License 2.0
//

package com.augusttechgroup.liquibase.delegate


import liquibase.changelog.ChangeSet
import liquibase.parser.ChangeLogParserFactory


class DatabaseChangeLogDelegate {
  def databaseChangeLog
  def params
  def resourceAccessor


  DatabaseChangeLogDelegate(databaseChangeLog) {
    this([:], databaseChangeLog)
  }

  
  DatabaseChangeLogDelegate(Map params, databaseChangeLog) {
    this.params = params
    this.databaseChangeLog = databaseChangeLog
    params.each { key, value ->
      databaseChangeLog[key] = value
    }
  }
  
  
  void changeSet(Map params, closure) {
    def changeSet = new ChangeSet(
      params.id,
      params.author,
      params.alwaysRun?.toBoolean() ?: false,
      params.runOnChange?.toBoolean() ?: false,
      databaseChangeLog.physicalFilePath,
      params.context,
      params.dbms,
      params.runInTransaction?.toBoolean() ?: true)

    if(params.failOnError) {
      changeSet.failOnError = params.failOnError?.toBoolean()
    }

    if(params.onValidationFail) {
      changeSet.onValidationFail = ChangeSet.ValidationFailOption.valueOf(params.onValidationFail)
    }

    def delegate = new ChangeSetDelegate(changeSet: changeSet,
                                         databaseChangeLog: databaseChangeLog,
                                         resourceAccessor: resourceAccessor)
    closure.delegate = delegate
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()
    
    databaseChangeLog.addChangeSet(changeSet)
  }


  void preConditions(Map params = [:], Closure closure) {
    databaseChangeLog.preconditions = PreconditionDelegate.buildPreconditionContainer(params, closure)
  }


  void include(Map params = [:]) {
    def physicalChangeLogLocation = databaseChangeLog.getFilePath().replace(System.getProperty("user.dir").toString() + "/", "")
    def relativeToChangelogFile = false
    if(params.relativeToChangelogFile){
      relativeToChangelogFile = params.relativeToChangelogFile
    }
    if(params.file) {
    if (relativeToChangelogFile && (physicalChangeLogLocation.contains("/") || physicalChangeLogLocation.contains("\\\\"))){
      params.file = physicalChangeLogLocation.replaceFirst("/[^/]*\$","") + "/" + params.file
    }
      includeChangeLog(params.file)
    }
    else if(params.path) {
    if (relativeToChangelogFile && (physicalChangeLogLocation.contains("/") || physicalChangeLogLocation.contains("\\\\"))){
      params.path = physicalChangeLogLocation.replaceFirst("/[^/]*\$","") + "/" + params.path	
    }
      def files = []
      new File(params.path).eachFileMatch(~/.*.groovy/) { file->
        files << file.path
      }

      files.sort().each { filename ->
        includeChangeLog(filename)
      }
    }
  }


  private def includeChangeLog(filename) {
    def parser = ChangeLogParserFactory.getInstance().getParser(filename, resourceAccessor)
    def includedChangeLog = parser.parse(filename, null, resourceAccessor)
    includedChangeLog?.changeSets.each { changeSet ->
      databaseChangeLog.addChangeSet(changeSet)
    }
    includedChangeLog?.preconditionContainer?.nestedPreconditions.each { precondition ->
      databaseChangeLog.preconditionContainer.addNestedPrecondition(precondition)
    }
  }


  void property(Map params = [:]) {
    
  }

}
