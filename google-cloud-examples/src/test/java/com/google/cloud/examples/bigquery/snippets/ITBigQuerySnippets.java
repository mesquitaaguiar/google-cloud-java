/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.examples.bigquery.snippets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.Page;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQuery.DatasetDeleteOption;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Field.Type;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryResult;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.cloud.bigquery.testing.RemoteBigQueryHelper;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ITBigQuerySnippets {

  private static final String DATASET = RemoteBigQueryHelper.generateDatasetName();
  private static final String OTHER_DATASET = RemoteBigQueryHelper.generateDatasetName();
  private static final String QUERY =
      "SELECT unique(corpus) FROM [bigquery-public-data:samples.shakespeare]";
  private static final Function<Job, JobId> TO_JOB_ID_FUNCTION = new Function<Job, JobId>() {
    @Override
    public JobId apply(Job job) {
      return job.jobId();
    }
  };
  private static final Function<Table, TableId> TO_TABLE_ID_FUNCTION =
      new Function<Table, TableId>() {
        @Override
        public TableId apply(Table table) {
          return table.tableId();
        }
      };
  private static final Function<Dataset, DatasetId> TO_DATASET_ID_FUNCTION =
      new Function<Dataset, DatasetId>() {
        @Override
        public DatasetId apply(Dataset dataset) {
          return dataset.datasetId();
        }
      };

  private static BigQuery bigquery;
  private static BigQuerySnippets bigquerySnippets;

  @Rule
  public Timeout globalTimeout = Timeout.seconds(300);

  @BeforeClass
  public static void beforeClass() {
    bigquery = RemoteBigQueryHelper.create().options().service();
    bigquerySnippets = new BigQuerySnippets(bigquery);
    bigquery.create(DatasetInfo.builder(DATASET).build());
  }

  @AfterClass
  public static void afterClass() throws ExecutionException, InterruptedException {
    bigquery.delete(DATASET, DatasetDeleteOption.deleteContents());
    bigquery.delete(OTHER_DATASET, DatasetDeleteOption.deleteContents());
  }

  @Test
  public void testCreateGetAndDeleteTable() throws InterruptedException {
    String tableName = "test_create_get_delete";
    String fieldName = "aField";
    Table table = bigquerySnippets.createTable(DATASET, tableName, fieldName);
    assertNotNull(table);
    TableId tableId = TableId.of(bigquery.options().projectId(), DATASET, tableName);
    assertEquals(tableId, bigquerySnippets.getTable(tableId.dataset(), tableId.table()).tableId());
    assertNotNull(bigquerySnippets.updateTable(DATASET, tableName, "new friendly name"));
    assertEquals("new friendly name",
        bigquerySnippets.getTableFromId(tableId.project(), tableId.dataset(), tableId.table())
            .friendlyName());
    Set<TableId> tables = Sets.newHashSet(
        Iterators.transform(bigquerySnippets.listTables(DATASET).iterateAll(),
        TO_TABLE_ID_FUNCTION));
    while (!tables.contains(tableId)) {
      Thread.sleep(500);
      tables = Sets.newHashSet(
          Iterators.transform(bigquerySnippets.listTables(DATASET).iterateAll(),
              TO_TABLE_ID_FUNCTION));
    }
    tables = Sets.newHashSet(Iterators.transform(
        bigquerySnippets.listTablesFromId(tableId.project(), DATASET).iterateAll(),
        TO_TABLE_ID_FUNCTION));
    while (!tables.contains(tableId)) {
      Thread.sleep(500);
      tables = Sets.newHashSet(Iterators.transform(
          bigquerySnippets.listTablesFromId(tableId.project(), DATASET).iterateAll(),
          TO_TABLE_ID_FUNCTION));
    }
    assertTrue(bigquerySnippets.deleteTable(DATASET, tableName));
    assertFalse(bigquerySnippets.deleteTableFromId(tableId.project(), DATASET, tableName));
  }

  @Test
  public void testCreateGetAndDeleteDataset() throws InterruptedException {
    DatasetId datasetId = DatasetId.of(bigquery.options().projectId(), OTHER_DATASET);
    Dataset dataset = bigquerySnippets.createDataset(OTHER_DATASET);
    assertNotNull(dataset);
    assertEquals(datasetId, bigquerySnippets.getDataset(OTHER_DATASET).datasetId());
    assertNotNull(bigquerySnippets.updateDataset(OTHER_DATASET, "new friendly name"));
    assertEquals("new friendly name",
        bigquerySnippets.getDatasetFromId(datasetId.project(), OTHER_DATASET).friendlyName());
    Set<DatasetId> datasets = Sets.newHashSet(
        Iterators.transform(bigquerySnippets.listDatasets().iterateAll(),
            TO_DATASET_ID_FUNCTION));
    while (!datasets.contains(datasetId)) {
      Thread.sleep(500);
      datasets = Sets.newHashSet(
          Iterators.transform(bigquerySnippets.listDatasets().iterateAll(),
              TO_DATASET_ID_FUNCTION));
    }
    datasets = Sets.newHashSet(
        Iterators.transform(bigquerySnippets.listDatasets(datasetId.project()).iterateAll(),
            TO_DATASET_ID_FUNCTION));
    while (!datasets.contains(datasetId)) {
      Thread.sleep(500);
      datasets = Sets.newHashSet(
          Iterators.transform(bigquerySnippets.listDatasets(datasetId.project()).iterateAll(),
              TO_DATASET_ID_FUNCTION));
    }
    assertTrue(bigquerySnippets.deleteDataset(OTHER_DATASET));
    assertFalse(bigquerySnippets.deleteDatasetFromId(datasetId.project(), OTHER_DATASET));
  }

  @Test
  public void testWriteAndListTableData() throws IOException, InterruptedException {
    String tableName = "test_write_and_list_table_data";
    String fieldName = "string_field";
    assertNotNull(bigquerySnippets.createTable(DATASET, tableName, fieldName));
    bigquerySnippets.writeToTable(DATASET, tableName, "StringValue1\nStringValue2\n");
    Page<List<FieldValue>> listPage = bigquerySnippets.listTableData(DATASET, tableName);
    while (Iterators.size(listPage.iterateAll()) < 2) {
      Thread.sleep(500);
      listPage = bigquerySnippets.listTableData(DATASET, tableName);
    }
    Iterator<List<FieldValue>> rowIterator = listPage.values().iterator();
    assertEquals("StringValue1", rowIterator.next().get(0).stringValue());
    assertEquals("StringValue2", rowIterator.next().get(0).stringValue());
    assertTrue(bigquerySnippets.deleteTable(DATASET, tableName));
  }

  @Test
  public void testInsertAllAndListTableData() throws IOException, InterruptedException {
    String tableName = "test_insert_all_and_list_table_data";
    String fieldName1 = "booleanField";
    String fieldName2 = "bytesField";
    TableId tableId = TableId.of(DATASET, tableName);
    Schema schema =
        Schema.of(Field.of(fieldName1, Type.bool()), Field.of(fieldName2, Type.bytes()));
    TableInfo table = TableInfo.of(tableId, StandardTableDefinition.of(schema));
    assertNotNull(bigquery.create(table));
    InsertAllResponse response = bigquerySnippets.insertAll(DATASET, tableName);
    assertFalse(response.hasErrors());
    assertTrue(response.insertErrors().isEmpty());
    Page<List<FieldValue>> listPage = bigquerySnippets.listTableDataFromId(DATASET, tableName);
    while (Iterators.size(listPage.iterateAll()) < 1) {
      Thread.sleep(500);
      listPage = bigquerySnippets.listTableDataFromId(DATASET, tableName);
    }
    List<FieldValue> row = listPage.values().iterator().next();
    assertEquals(true, row.get(0).booleanValue());
    assertArrayEquals(new byte[]{0xD, 0xE, 0xA, 0xD}, row.get(1).bytesValue());
    assertTrue(bigquerySnippets.deleteTable(DATASET, tableName));
  }

  @Test
  public void testJob() throws ExecutionException, InterruptedException {
    Job job1 = bigquerySnippets.createJob(QUERY);
    Job job2 = bigquerySnippets.createJob(QUERY);
    assertNotNull(job1);
    assertNotNull(job2);
    assertEquals(job1.jobId(), bigquerySnippets.getJob(job1.jobId().job()).jobId());
    assertEquals(job2.jobId(), bigquerySnippets.getJobFromId(job2.jobId().job()).jobId());
    Set<JobId> jobs = Sets.newHashSet(Iterators.transform(bigquerySnippets.listJobs().iterateAll(),
        TO_JOB_ID_FUNCTION));
    while (!jobs.contains(job1.jobId()) || !jobs.contains(job2.jobId())) {
      Thread.sleep(500);
      jobs = Sets.newHashSet(Iterators.transform(bigquerySnippets.listJobs().iterateAll(),
          TO_JOB_ID_FUNCTION));
    }
    assertTrue(bigquerySnippets.cancelJob(job1.jobId().job()));
    assertTrue(bigquerySnippets.cancelJobFromId(job2.jobId().job()));
  }

  @Test
  public void testRunQuery() throws InterruptedException {
    QueryResponse queryResponse = bigquerySnippets.runQuery(QUERY);
    assertNotNull(queryResponse);
    assertTrue(queryResponse.jobCompleted());
    assertFalse(queryResponse.hasErrors());
    QueryResult result = queryResponse.result();
    assertNotNull(result);
    assertTrue(bigquerySnippets.cancelJob(queryResponse.jobId().job()));
    queryResponse = bigquerySnippets.queryResults(QUERY);
    assertNotNull(queryResponse);
    assertTrue(queryResponse.jobCompleted());
    assertFalse(queryResponse.hasErrors());
    result = queryResponse.result();
    assertNotNull(result);
    assertTrue(bigquerySnippets.cancelJobFromId(queryResponse.jobId().job()));
  }
}