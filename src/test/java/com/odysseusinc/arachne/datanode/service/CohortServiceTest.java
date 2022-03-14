/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: June 22, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service;


import static com.odysseusinc.arachne.commons.types.DBMSType.MS_SQL_SERVER;
import static com.odysseusinc.arachne.commons.types.DBMSType.ORACLE;
import static com.odysseusinc.arachne.commons.types.DBMSType.POSTGRESQL;
import static com.odysseusinc.arachne.commons.types.DBMSType.REDSHIFT;

import com.odysseusinc.arachne.datanode.repository.AtlasRepository;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.service.impl.CohortServiceImpl;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ohdsi.sql.SqlTranslate;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.ClassPathResource;

@RunWith(MockitoJUnitRunner.class)
public class CohortServiceTest {

    private static final String TEMP_SCHEMA = "tempSchema";
    private static final String SESSION_ID = SqlTranslate.generateSessionId();

    private static String REDSHIFT_SQL_RESULT;

    private static CohortServiceImpl.TranslateOptions options
            = new CohortServiceImpl.TranslateOptions("public", "public", "public", "public","cohort", 1);

    @Mock
    private CentralSystemClient centralClient;
    @Mock
    private ConfigurableListableBeanFactory beanFactory;
    @Mock
    private AtlasRepository atlasRepository;
    @Mock
    private DataNodeService dataNodeService;

    @InjectMocks
    private CohortServiceImpl cohortService ;

    public CohortServiceTest() throws IOException {

        REDSHIFT_SQL_RESULT = IOUtils.toString(new ClassPathResource("data/sqlRender/etalon.redshift.sql").getInputStream(), StandardCharsets.UTF_8);
    }

    @Test
    public void createPostgresSQLTest() {

        final String sql = cohortService.translateSQL(MS_SQL_SQL_RESULT, null, POSTGRESQL, SESSION_ID, TEMP_SCHEMA, options);
        assertSqlEquals(POSTGRES_SQL_RESULT, sql);
    }

    @Test
    public void createRedshiftSQLTest(){

        final String sql = cohortService.translateSQL(MS_SQL_SQL_RESULT, null, REDSHIFT, SESSION_ID, TEMP_SCHEMA,options);
        assertSqlEquals(REDSHIFT_SQL_RESULT, sql);
    }

    @Ignore("unexpected temp schema names")
    @Test
    public void createOracleSQLTest() {

        final String sql = cohortService.translateSQL(MS_SQL_SQL_RESULT, null, ORACLE, SESSION_ID, TEMP_SCHEMA,options);
        assertSqlEquals(ORACLE_SQL_RESULT, sql);
    }

    @Test
    public void createMSSQLTest(){

        final String sql = cohortService.translateSQL(MS_SQL_SQL_RESULT, null, MS_SQL_SERVER, SESSION_ID, TEMP_SCHEMA,options);
        assertSqlEquals(MS_SQL_SQL_RESULT, sql);
    }

    private static final String POSTGRES_SQL_RESULT = "CREATE TEMP TABLE Codesets  (codeset_id int NOT NULL,\n" +
            "  concept_id bigint NOT NULL\n" +
            ")\n" +
            ";\n" +
            "\n" +
            "INSERT INTO Codesets (codeset_id, concept_id)\n" +
            "SELECT 0 as codeset_id, c.concept_id FROM (select distinct I.concept_id FROM\n" +
            "( \n" +
            "  select concept_id from public.CONCEPT where 0=1\n" +
            ") I\n" +
            ") C;\n" +
            "INSERT INTO Codesets (codeset_id, concept_id)\n" +
            "SELECT 1 as codeset_id, c.concept_id FROM (select distinct I.concept_id FROM\n" +
            "( \n" +
            "  select concept_id from public.CONCEPT where 0=1\n" +
            ") I\n" +
            ") C;\n" +
            "\n" +
            "\n" +
            "CREATE TEMP TABLE qualified_events\n" +
            "\n" +
            "AS\n" +
            "WITH primary_events (event_id, person_id, start_date, end_date, op_start_date, op_end_date)  AS (\n" +
            "-- Begin Primary Events\n" +
            "select row_number() over (PARTITION BY P.person_id order by P.start_date) as event_id, P.person_id, P.start_date, P.end_date, OP.observation_period_start_date as op_start_date, OP.observation_period_end_date as op_end_date\n" +
            "FROM\n" +
            "(\n" +
            "  select P.person_id, P.start_date, P.end_date, ROW_NUMBER() OVER (PARTITION BY person_id ORDER BY start_date ASC) ordinal\n" +
            "  FROM \n" +
            "  (\n" +
            "  -- Begin Drug Era Criteria\n" +
            "select C.person_id, C.drug_era_id as event_id, C.drug_era_start_date as start_date, C.drug_era_end_date as end_date, C.drug_concept_id as TARGET_CONCEPT_ID\n" +
            "from \n" +
            "(\n" +
            "  select de.*, ROW_NUMBER() over (PARTITION BY de.person_id ORDER BY de.drug_era_start_date, de.drug_era_id) as ordinal\n" +
            "  FROM public.DRUG_ERA de\n" +
            "where de.drug_concept_id in (SELECT concept_id from  Codesets where codeset_id = 0)\n" +
            ") C\n" +
            "\n" +
            "\n" +
            "-- End Drug Era Criteria\n" +
            "\n" +
            "  ) P\n" +
            ") P\n" +
            "JOIN public.observation_period OP on P.person_id = OP.person_id and P.start_date >=  OP.observation_period_start_date and P.start_date <= op.observation_period_end_date\n" +
            "WHERE (OP.OBSERVATION_PERIOD_START_DATE + 7*INTERVAL'1 day') <= P.START_DATE AND (P.START_DATE + 30*INTERVAL'1 day') <= OP.OBSERVATION_PERIOD_END_DATE AND P.ordinal = 1\n" +
            "-- End Primary Events\n" +
            "\n" +
            ")\n" +
            " SELECT\n" +
            "event_id, person_id, start_date, end_date, op_start_date, op_end_date\n" +
            "\n" +
            "FROM\n" +
            "(\n" +
            "  select pe.event_id, pe.person_id, pe.start_date, pe.end_date, pe.op_start_date, pe.op_end_date, row_number() over (partition by pe.person_id order by pe.start_date DESC) as ordinal\n" +
            "  FROM primary_events pe\n" +
            "  \n" +
            "JOIN (\n" +
            "-- Begin Criteria Group\n" +
            "select 0 as index_id, person_id, event_id\n" +
            "FROM\n" +
            "(\n" +
            "  select E.person_id, E.event_id \n" +
            "  FROM primary_events E\n" +
            "  LEFT JOIN\n" +
            "  (\n" +
            "    -- Begin Correlated Criteria\n" +
            "SELECT 0 as index_id, p.person_id, p.event_id\n" +
            "FROM primary_events P\n" +
            "LEFT JOIN\n" +
            "(\n" +
            "  -- Begin Condition Era Criteria\n" +
            "select C.person_id, C.condition_era_id as event_id, C.condition_era_start_date as start_date, C.condition_era_end_date as end_date, C.CONDITION_CONCEPT_ID as TARGET_CONCEPT_ID\n" +
            "from \n" +
            "(\n" +
            "  select ce.*, ROW_NUMBER() over (PARTITION BY ce.person_id ORDER BY ce.condition_era_start_date, ce.condition_era_id) as ordinal\n" +
            "  FROM public.CONDITION_ERA ce\n" +
            "where ce.condition_concept_id in (SELECT concept_id from  Codesets where codeset_id = 1)\n" +
            ") C\n" +
            "\n" +
            "\n" +
            "-- End Condition Era Criteria\n" +
            "\n" +
            ") A on A.person_id = P.person_id and A.START_DATE >= P.OP_START_DATE AND A.START_DATE <= P.OP_END_DATE AND A.START_DATE >= (P.START_DATE + 1*INTERVAL'1 day') and A.START_DATE <= (P.START_DATE + 30*INTERVAL'1 day')\n" +
            "GROUP BY p.person_id, p.event_id\n" +
            "HAVING COUNT(A.TARGET_CONCEPT_ID) >= 1\n" +
            "-- End Correlated Criteria\n" +
            "\n" +
            "  ) CQ on E.person_id = CQ.person_id and E.event_id = CQ.event_id\n" +
            "  GROUP BY E.person_id, E.event_id\n" +
            "  HAVING COUNT(index_id) = 1\n" +
            ") G\n" +
            "-- End Criteria Group\n" +
            ") AC on AC.person_id = pe.person_id and AC.event_id = pe.event_id\n" +
            "\n" +
            ") QE\n" +
            "WHERE QE.ordinal = 1\n" +
            ";\n" +
            "ANALYZE qualified_events\n" +
            ";\n\n\n" +
            "CREATE TEMP TABLE inclusionRuleCohorts \n" +
            " (inclusion_rule_id bigint,\n" +
            "  person_id bigint,\n" +
            "  event_id bigint\n" +
            ")\n" +
            ";\n" +
            "\n" +
            "\n" +
            "CREATE TEMP TABLE included_events\n" +
            "\n" +
            "AS\n" +
            "WITH cteIncludedEvents(event_id, person_id, start_date, end_date, op_start_date, op_end_date, ordinal)  AS (\n" +
            "  SELECT event_id, person_id, start_date, end_date, op_start_date, op_end_date, row_number() over (partition by person_id order by start_date ASC) as ordinal\n" +
            "  from\n" +
            "  (\n" +
            "    select Q.event_id, Q.person_id, Q.start_date, Q.end_date, Q.op_start_date, Q.op_end_date, SUM(coalesce(POWER(cast(2 as bigint), I.inclusion_rule_id), 0)) as inclusion_rule_mask\n" +
            "    from qualified_events Q\n" +
            "    LEFT JOIN inclusionRuleCohorts I on I.person_id = Q.person_id and I.event_id = Q.event_id\n" +
            "    GROUP BY Q.event_id, Q.person_id, Q.start_date, Q.end_date, Q.op_start_date, Q.op_end_date\n" +
            "  ) MG -- matching groups\n" +
            "\n" +
            ")\n" +
            " SELECT\n" +
            "event_id, person_id, start_date, end_date, op_start_date, op_end_date\n" +
            "\n" +
            "FROM\n" +
            "cteIncludedEvents Results\n" +
            "WHERE Results.ordinal = 1\n" +
            ";\n" +
            "ANALYZE included_events\n" +
            ";\n\n" +
            "-- Apply end date stratagies\n" +
            "-- by default, all events extend to the op_end_date.\n" +
            "CREATE TEMP TABLE cohort_ends\n" +
            "\n" +
            "AS\n" +
            "SELECT\n" +
            "event_id, person_id, op_end_date as end_date\n" +
            "\n" +
            "FROM\n" +
            "included_events;\n" +
            "ANALYZE cohort_ends\n" +
            ";\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "DELETE FROM public.arachne_cohorts where cohort_definition_id = 1;\n" +
            "INSERT INTO public.arachne_cohorts (cohort_definition_id, subject_id, cohort_start_date, cohort_end_date)\n" +
            "select 1 as cohort_definition_id, F.person_id, F.start_date, F.end_date\n" +
            "FROM (\n" +
            "  select I.person_id, I.start_date, E.end_date, row_number() over (partition by I.person_id, I.event_id order by E.end_date) as ordinal \n" +
            "  from included_events I\n" +
            "  join cohort_ends E on I.event_id = E.event_id and I.person_id = E.person_id and E.end_date >= I.start_date\n" +
            ") F\n" +
            "WHERE F.ordinal = 1\n" +
            ";\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "TRUNCATE TABLE cohort_ends;\n" +
            "DROP TABLE cohort_ends;\n" +
            "\n" +
            "TRUNCATE TABLE inclusionRuleCohorts;\n" +
            "DROP TABLE inclusionRuleCohorts;\n" +
            "\n" +
            "TRUNCATE TABLE qualified_events;\n" +
            "DROP TABLE qualified_events;\n" +
            "\n" +
            "TRUNCATE TABLE included_events;\n" +
            "DROP TABLE included_events;\n" +
            "\n" +
            "TRUNCATE TABLE Codesets;\n" +
            "DROP TABLE Codesets";

    private static final String ORACLE_SQL_RESULT = "CREATE TABLE se3jtcbfCodesets (\n" +
            "  codeset_id int NOT NULL,\n" +
            "  concept_id NUMBER(19) NOT NULL\n" +
            ")\n" +
            ";\n" +
            "\n" +
            "INSERT INTO se3jtcbfCodesets (codeset_id, concept_id)\n" +
            "SELECT   0 as codeset_id, c.concept_id  FROM  (SELECT   distinct I.concept_id  FROM \n" +
            "(SELECT     concept_id   FROM   public.CONCEPT   WHERE  0=1\n" +
            " ) I\n" +
            " ) C ;\n" +
            "INSERT INTO se3jtcbfCodesets (codeset_id, concept_id)\n" +
            "SELECT   1 as codeset_id, c.concept_id  FROM  (SELECT   distinct I.concept_id  FROM \n" +
            "(SELECT     concept_id   FROM   public.CONCEPT   WHERE  0=1\n" +
            " ) I\n" +
            " ) C ;\n" +
            "\n" +
            "\n" +
            "CREATE TABLE  se3jtcbfqualified_events\n" +
            "  \n" +
            "AS\n" +
            "WITH  primary_events (event_id, person_id, start_date, end_date, op_start_date, op_end_date)  AS \n" +
            "(SELECT     row_number() over (PARTITION BY P.person_id order by P.start_date) as event_id, P.person_id, P.start_date, P.end_date, OP.observation_period_start_date as op_start_date, OP.observation_period_end_date as op_end_date\n" +
            "  FROM  \n" +
            "(SELECT   P.person_id, P.start_date, P.end_date, ROW_NUMBER() OVER (PARTITION BY person_id ORDER BY start_date ASC) ordinal\n" +
            "   FROM  \n" +
            "  (SELECT   C.person_id, C.drug_era_id as event_id, C.drug_era_start_date as start_date, C.drug_era_end_date as end_date, C.drug_concept_id as TARGET_CONCEPT_ID\n" +
            " FROM  \n" +
            "(SELECT     de.*, ROW_NUMBER() over (PARTITION BY de.person_id ORDER BY de.drug_era_start_date, de.drug_era_id) as ordinal\n" +
            "    FROM   public.DRUG_ERA de\n" +
            "  WHERE  de.drug_concept_id in (SELECT     concept_id   FROM    se3jtcbfCodesets   WHERE  codeset_id = 0 )\n" +
            " ) C\n" +
            "\n" +
            "\n" +
            "-- End Drug Era Criteria\n" +
            "\n" +
            "   ) P\n" +
            " ) P\n" +
            "JOIN public.observation_period OP on P.person_id = OP.person_id and P.start_date >=  OP.observation_period_start_date and P.start_date <= op.observation_period_end_date\n" +
            "  WHERE  (OP.OBSERVATION_PERIOD_START_DATE + 7) <= P.START_DATE AND (P.START_DATE + 30) <= OP.OBSERVATION_PERIOD_END_DATE AND P.ordinal = 1\n" +
            "-- End Primary Events\n" +
            "\n" +
            " )\n" +
            " SELECT\n" +
            "     event_id, person_id, start_date, end_date, op_start_date, op_end_date\n" +
            "\n" +
            "FROM\n" +
            "   \n" +
            "(SELECT   pe.event_id, pe.person_id, pe.start_date, pe.end_date, pe.op_start_date, pe.op_end_date, row_number() over (partition by pe.person_id order by pe.start_date DESC) as ordinal\n" +
            "   FROM  primary_events pe\n" +
            "  \n" +
            "JOIN (SELECT   0 as index_id, person_id, event_id\n" +
            " FROM \n" +
            "(SELECT   E.person_id, E.event_id \n" +
            "   FROM  primary_events E\n" +
            "  LEFT JOIN\n" +
            "  (SELECT   0 as index_id, p.person_id, p.event_id\n" +
            " FROM  primary_events P\n" +
            "LEFT JOIN\n" +
            "(SELECT   C.person_id, C.condition_era_id as event_id, C.condition_era_start_date as start_date, C.condition_era_end_date as end_date, C.CONDITION_CONCEPT_ID as TARGET_CONCEPT_ID\n" +
            " FROM  \n" +
            "(SELECT     ce.*, ROW_NUMBER() over (PARTITION BY ce.person_id ORDER BY ce.condition_era_start_date, ce.condition_era_id) as ordinal\n" +
            "    FROM   public.CONDITION_ERA ce\n" +
            "  WHERE  ce.condition_concept_id in (SELECT     concept_id   FROM    se3jtcbfCodesets   WHERE  codeset_id = 1 )\n" +
            " ) C\n" +
            "\n" +
            "\n" +
            "-- End Condition Era Criteria\n" +
            "\n" +
            " ) A on A.person_id = P.person_id and A.START_DATE >= P.OP_START_DATE AND A.START_DATE <= P.OP_END_DATE AND A.START_DATE >= (P.START_DATE + 1) and A.START_DATE <= (P.START_DATE + 30)\n" +
            "GROUP BY p.person_id, p.event_id\n" +
            "HAVING COUNT(A.TARGET_CONCEPT_ID) >= 1\n" +
            "-- End Correlated Criteria\n" +
            "\n" +
            "   ) CQ on E.person_id = CQ.person_id and E.event_id = CQ.event_id\n" +
            "  GROUP BY E.person_id, E.event_id\n" +
            "  HAVING COUNT(index_id) = 1\n" +
            " ) G\n" +
            "-- End Criteria Group\n" +
            " ) AC on AC.person_id = pe.person_id and AC.event_id = pe.event_id\n" +
            "\n" +
            " ) QE\n" +
            "  WHERE  QE.ordinal = 1\n" +
            " ;\n" +
            "\n" +
            "\n" +
            "create table se3jtcbfinclusionRuleCohorts \n" +
            "(\n" +
            "  inclusion_rule_id NUMBER(19),\n" +
            "  person_id NUMBER(19),\n" +
            "  event_id NUMBER(19)\n" +
            ")\n" +
            ";\n" +
            "\n" +
            "\n" +
            "CREATE TABLE  se3jtcbfincluded_events\n" +
            "  \n" +
            "AS\n" +
            "WITH  cteIncludedEvents(event_id, person_id, start_date, end_date, op_start_date, op_end_date, ordinal)  AS \n" +
            "(SELECT   event_id, person_id, start_date, end_date, op_start_date, op_end_date, row_number() over (partition by person_id order by start_date ASC) as ordinal\n" +
            "   FROM \n" +
            "  (SELECT   Q.event_id, Q.person_id, Q.start_date, Q.end_date, Q.op_start_date, Q.op_end_date, SUM(coalesce(POWER(cast(2 as NUMBER(19)), I.inclusion_rule_id), 0)) as inclusion_rule_mask\n" +
            "     FROM  se3jtcbfqualified_events Q\n" +
            "    LEFT JOIN se3jtcbfinclusionRuleCohorts I on I.person_id = Q.person_id and I.event_id = Q.event_id\n" +
            "    GROUP BY Q.event_id, Q.person_id, Q.start_date, Q.end_date, Q.op_start_date, Q.op_end_date\n" +
            "   ) MG -- matching groups\n" +
            "\n" +
            " )\n" +
            " SELECT\n" +
            "     event_id, person_id, start_date, end_date, op_start_date, op_end_date\n" +
            "\n" +
            "FROM\n" +
            "   cteIncludedEvents Results\n" +
            "  WHERE  Results.ordinal = 1\n" +
            " ;\n" +
            "\n" +
            "-- Apply end date stratagies\n" +
            "-- by default, all events extend to the op_end_date.\n" +
            "CREATE TABLE  se3jtcbfcohort_ends\n" +
            "  AS\n" +
            "SELECT\n" +
            "   event_id, person_id, op_end_date as end_date\n" +
            "\n" +
            "FROM\n" +
            "  se3jtcbfincluded_events ;\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "DELETE FROM public.arachne_cohorts where cohort_definition_id = 1;\n" +
            "INSERT INTO public.arachne_cohorts (cohort_definition_id, subject_id, cohort_start_date, cohort_end_date)\n" +
            "SELECT     1 as cohort_definition_id, F.person_id, F.start_date, F.end_date\n" +
            "  FROM   (SELECT   I.person_id, I.start_date, E.end_date, row_number() over (partition by I.person_id, I.event_id order by E.end_date) as ordinal \n" +
            "   FROM  se3jtcbfincluded_events I\n" +
            "  join se3jtcbfcohort_ends E on I.event_id = E.event_id and I.person_id = E.person_id and E.end_date >= I.start_date\n" +
            " ) F\n" +
            "  WHERE  F.ordinal = 1\n" +
            " ;\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "TRUNCATE TABLE se3jtcbfcohort_ends;\n" +
            "DROP TABLE se3jtcbfcohort_ends;\n" +
            "\n" +
            "TRUNCATE TABLE se3jtcbfinclusionRuleCohorts;\n" +
            "DROP TABLE se3jtcbfinclusionRuleCohorts;\n" +
            "\n" +
            "TRUNCATE TABLE se3jtcbfqualified_events;\n" +
            "DROP TABLE se3jtcbfqualified_events;\n" +
            "\n" +
            "TRUNCATE TABLE se3jtcbfincluded_events;\n" +
            "DROP TABLE se3jtcbfincluded_events;\n" +
            "\n" +
            "TRUNCATE TABLE se3jtcbfCodesets;\n" +
            "DROP TABLE se3jtcbfCodesets";

    private static final String MS_SQL_SQL_RESULT = "CREATE TABLE #Codesets (\n" +
            "  codeset_id int NOT NULL,\n" +
            "  concept_id bigint NOT NULL\n" +
            ")\n" +
            ";\n" +
            "\n" +
            "INSERT INTO #Codesets (codeset_id, concept_id)\n" +
            "SELECT 0 as codeset_id, c.concept_id FROM (select distinct I.concept_id FROM\n" +
            "( \n" +
            "  select concept_id from public.CONCEPT where 0=1\n" +
            ") I\n" +
            ") C;\n" +
            "INSERT INTO #Codesets (codeset_id, concept_id)\n" +
            "SELECT 1 as codeset_id, c.concept_id FROM (select distinct I.concept_id FROM\n" +
            "( \n" +
            "  select concept_id from public.CONCEPT where 0=1\n" +
            ") I\n" +
            ") C;\n" +
            "\n" +
            "\n" +
            "with primary_events (event_id, person_id, start_date, end_date, op_start_date, op_end_date) as\n" +
            "(\n" +
            "-- Begin Primary Events\n" +
            "select row_number() over (PARTITION BY P.person_id order by P.start_date) as event_id, P.person_id, P.start_date, P.end_date, OP.observation_period_start_date as op_start_date, OP.observation_period_end_date as op_end_date\n" +
            "FROM\n" +
            "(\n" +
            "  select P.person_id, P.start_date, P.end_date, ROW_NUMBER() OVER (PARTITION BY person_id ORDER BY start_date ASC) ordinal\n" +
            "  FROM \n" +
            "  (\n" +
            "  -- Begin Drug Era Criteria\n" +
            "select C.person_id, C.drug_era_id as event_id, C.drug_era_start_date as start_date, C.drug_era_end_date as end_date, C.drug_concept_id as TARGET_CONCEPT_ID\n" +
            "from \n" +
            "(\n" +
            "  select de.*, ROW_NUMBER() over (PARTITION BY de.person_id ORDER BY de.drug_era_start_date, de.drug_era_id) as ordinal\n" +
            "  FROM public.DRUG_ERA de\n" +
            "where de.drug_concept_id in (SELECT concept_id from  #Codesets where codeset_id = 0)\n" +
            ") C\n" +
            "\n" +
            "\n" +
            "-- End Drug Era Criteria\n" +
            "\n" +
            "  ) P\n" +
            ") P\n" +
            "JOIN public.observation_period OP on P.person_id = OP.person_id and P.start_date >=  OP.observation_period_start_date and P.start_date <= op.observation_period_end_date\n" +
            "WHERE DATEADD(day,7,OP.OBSERVATION_PERIOD_START_DATE) <= P.START_DATE AND DATEADD(day,30,P.START_DATE) <= OP.OBSERVATION_PERIOD_END_DATE AND P.ordinal = 1\n" +
            "-- End Primary Events\n" +
            "\n" +
            ")\n" +
            "SELECT event_id, person_id, start_date, end_date, op_start_date, op_end_date\n" +
            "INTO #qualified_events\n" +
            "FROM \n" +
            "(\n" +
            "  select pe.event_id, pe.person_id, pe.start_date, pe.end_date, pe.op_start_date, pe.op_end_date, row_number() over (partition by pe.person_id order by pe.start_date DESC) as ordinal\n" +
            "  FROM primary_events pe\n" +
            "  \n" +
            "JOIN (\n" +
            "-- Begin Criteria Group\n" +
            "select 0 as index_id, person_id, event_id\n" +
            "FROM\n" +
            "(\n" +
            "  select E.person_id, E.event_id \n" +
            "  FROM primary_events E\n" +
            "  LEFT JOIN\n" +
            "  (\n" +
            "    -- Begin Correlated Criteria\n" +
            "SELECT 0 as index_id, p.person_id, p.event_id\n" +
            "FROM primary_events P\n" +
            "LEFT JOIN\n" +
            "(\n" +
            "  -- Begin Condition Era Criteria\n" +
            "select C.person_id, C.condition_era_id as event_id, C.condition_era_start_date as start_date, C.condition_era_end_date as end_date, C.CONDITION_CONCEPT_ID as TARGET_CONCEPT_ID\n" +
            "from \n" +
            "(\n" +
            "  select ce.*, ROW_NUMBER() over (PARTITION BY ce.person_id ORDER BY ce.condition_era_start_date, ce.condition_era_id) as ordinal\n" +
            "  FROM public.CONDITION_ERA ce\n" +
            "where ce.condition_concept_id in (SELECT concept_id from  #Codesets where codeset_id = 1)\n" +
            ") C\n" +
            "\n" +
            "\n" +
            "-- End Condition Era Criteria\n" +
            "\n" +
            ") A on A.person_id = P.person_id and A.START_DATE >= P.OP_START_DATE AND A.START_DATE <= P.OP_END_DATE AND A.START_DATE >= DATEADD(day,1,P.START_DATE) and A.START_DATE <= DATEADD(day,30,P.START_DATE)\n" +
            "GROUP BY p.person_id, p.event_id\n" +
            "HAVING COUNT(A.TARGET_CONCEPT_ID) >= 1\n" +
            "-- End Correlated Criteria\n" +
            "\n" +
            "  ) CQ on E.person_id = CQ.person_id and E.event_id = CQ.event_id\n" +
            "  GROUP BY E.person_id, E.event_id\n" +
            "  HAVING COUNT(index_id) = 1\n" +
            ") G\n" +
            "-- End Criteria Group\n" +
            ") AC on AC.person_id = pe.person_id and AC.event_id = pe.event_id\n" +
            "\n" +
            ") QE\n" +
            "WHERE QE.ordinal = 1\n" +
            ";\n" +
            "\n" +
            "\n" +
            "create table #inclusionRuleCohorts \n" +
            "(\n" +
            "  inclusion_rule_id bigint,\n" +
            "  person_id bigint,\n" +
            "  event_id bigint\n" +
            ")\n" +
            ";\n" +
            "\n" +
            "\n" +
            "with cteIncludedEvents(event_id, person_id, start_date, end_date, op_start_date, op_end_date, ordinal) as\n" +
            "(\n" +
            "  SELECT event_id, person_id, start_date, end_date, op_start_date, op_end_date, row_number() over (partition by person_id order by start_date ASC) as ordinal\n" +
            "  from\n" +
            "  (\n" +
            "    select Q.event_id, Q.person_id, Q.start_date, Q.end_date, Q.op_start_date, Q.op_end_date, SUM(coalesce(POWER(cast(2 as bigint), I.inclusion_rule_id), 0)) as inclusion_rule_mask\n" +
            "    from #qualified_events Q\n" +
            "    LEFT JOIN #inclusionRuleCohorts I on I.person_id = Q.person_id and I.event_id = Q.event_id\n" +
            "    GROUP BY Q.event_id, Q.person_id, Q.start_date, Q.end_date, Q.op_start_date, Q.op_end_date\n" +
            "  ) MG -- matching groups\n" +
            "\n" +
            ")\n" +
            "select event_id, person_id, start_date, end_date, op_start_date, op_end_date\n" +
            "into #included_events\n" +
            "FROM cteIncludedEvents Results\n" +
            "WHERE Results.ordinal = 1\n" +
            ";\n" +
            "\n" +
            "-- Apply end date stratagies\n" +
            "-- by default, all events extend to the op_end_date.\n" +
            "select event_id, person_id, op_end_date as end_date\n" +
            "into #cohort_ends\n" +
            "from #included_events;\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "DELETE FROM public.arachne_cohorts where cohort_definition_id = 1;\n" +
            "INSERT INTO public.arachne_cohorts (cohort_definition_id, subject_id, cohort_start_date, cohort_end_date)\n" +
            "select 1 as cohort_definition_id, F.person_id, F.start_date, F.end_date\n" +
            "FROM (\n" +
            "  select I.person_id, I.start_date, E.end_date, row_number() over (partition by I.person_id, I.event_id order by E.end_date) as ordinal \n" +
            "  from #included_events I\n" +
            "  join #cohort_ends E on I.event_id = E.event_id and I.person_id = E.person_id and E.end_date >= I.start_date\n" +
            ") F\n" +
            "WHERE F.ordinal = 1\n" +
            ";\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "TRUNCATE TABLE #cohort_ends;\n" +
            "DROP TABLE #cohort_ends;\n" +
            "\n" +
            "TRUNCATE TABLE #inclusionRuleCohorts;\n" +
            "DROP TABLE #inclusionRuleCohorts;\n" +
            "\n" +
            "TRUNCATE TABLE #qualified_events;\n" +
            "DROP TABLE #qualified_events;\n" +
            "\n" +
            "TRUNCATE TABLE #included_events;\n" +
            "DROP TABLE #included_events;\n" +
            "\n" +
            "TRUNCATE TABLE #Codesets;\n" +
            "DROP TABLE #Codesets";

    private static void assertSqlEquals(String expected, String actual) {
        Assert.assertEquals(removeEmptyLines(expected), removeEmptyLines(actual));
    }

    private static String removeEmptyLines(String sql) {
        return sql.replaceAll("(?m)^[ \t]*\r?\n", "");
    }
}
