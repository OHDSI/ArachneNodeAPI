CREATE TABLE #Codesets  (codeset_id int NOT NULL,
  concept_id bigint NOT NULL
)
DISTSTYLE ALL;

INSERT INTO #Codesets (codeset_id, concept_id)
SELECT 0 as codeset_id, c.concept_id FROM (select distinct I.concept_id FROM
( 
  select concept_id from public.CONCEPT where 0=1
) I
) C;
INSERT INTO #Codesets (codeset_id, concept_id)
SELECT 1 as codeset_id, c.concept_id FROM (select distinct I.concept_id FROM
( 
  select concept_id from public.CONCEPT where 0=1
) I
) C;


CREATE TABLE #qualified_events

DISTKEY(person_id)
AS
WITH
primary_events (event_id, person_id, start_date, end_date, op_start_date, op_end_date) 
AS
(
-- Begin Primary Events
select ROW_NUMBER() OVER (PARTITION BY P.person_id  ORDER BY P.start_date ) as event_id, P.person_id, P.start_date, P.end_date, OP.observation_period_start_date as op_start_date, OP.observation_period_end_date as op_end_date
FROM
(
  select P.person_id, P.start_date, P.end_date, ROW_NUMBER() OVER (PARTITION BY person_id  ORDER BY start_date  ASC ) ordinal
  FROM 
  (
  -- Begin Drug Era Criteria
select C.person_id, C.drug_era_id as event_id, C.drug_era_start_date as start_date, C.drug_era_end_date as end_date, C.drug_concept_id as TARGET_CONCEPT_ID
from 
(
  select de.*, ROW_NUMBER() OVER (PARTITION BY de.person_id  ORDER BY de.drug_era_start_date, de.drug_era_id ) as ordinal
  FROM public.DRUG_ERA de
where de.drug_concept_id in (SELECT concept_id from  #Codesets where codeset_id = 0)
) C


-- End Drug Era Criteria

  ) P
) P
JOIN public.observation_period OP on P.person_id = OP.person_id and P.start_date >=  OP.observation_period_start_date and P.start_date <= op.observation_period_end_date
WHERE DATEADD(day,CAST(7 as int),OP.OBSERVATION_PERIOD_START_DATE) <= P.START_DATE AND DATEADD(day,CAST(30 as int),P.START_DATE) <= OP.OBSERVATION_PERIOD_END_DATE AND P.ordinal = 1
-- End Primary Events

)

SELECT
event_id,  person_id , start_date, end_date, op_start_date, op_end_date

FROM
(
  select pe.event_id, pe.person_id, pe.start_date, pe.end_date, pe.op_start_date, pe.op_end_date, ROW_NUMBER() OVER (partition by pe.person_id  ORDER BY pe.start_date  DESC ) as ordinal
  FROM primary_events pe
  
JOIN (
-- Begin Criteria Group
select 0 as index_id, person_id, event_id
FROM
(
  select E.person_id, E.event_id 
  FROM primary_events E
  LEFT JOIN
  (
    -- Begin Correlated Criteria
SELECT 0 as index_id, p.person_id, p.event_id
FROM primary_events P
LEFT JOIN
(
  -- Begin Condition Era Criteria
select C.person_id, C.condition_era_id as event_id, C.condition_era_start_date as start_date, C.condition_era_end_date as end_date, C.CONDITION_CONCEPT_ID as TARGET_CONCEPT_ID
from 
(
  select ce.*, ROW_NUMBER() OVER (PARTITION BY ce.person_id  ORDER BY ce.condition_era_start_date, ce.condition_era_id ) as ordinal
  FROM public.CONDITION_ERA ce
where ce.condition_concept_id in (SELECT concept_id from  #Codesets where codeset_id = 1)
) C


-- End Condition Era Criteria

) A on A.person_id = P.person_id and A.START_DATE >= P.OP_START_DATE AND A.START_DATE <= P.OP_END_DATE AND A.START_DATE >= DATEADD(day,CAST(1 as int),P.START_DATE) and A.START_DATE <= DATEADD(day,CAST(30 as int),P.START_DATE)
GROUP BY p.person_id, p.event_id
HAVING COUNT(A.TARGET_CONCEPT_ID) >= 1
-- End Correlated Criteria

  ) CQ on E.person_id = CQ.person_id and E.event_id = CQ.event_id
  GROUP BY E.person_id, E.event_id
  HAVING COUNT(index_id) = 1
) G
-- End Criteria Group
) AC on AC.person_id = pe.person_id and AC.event_id = pe.event_id

) QE
WHERE QE.ordinal = 1
;


CREATE TABLE #inclusionRuleCohorts 
 (inclusion_rule_id bigint,
   person_id bigint,
  event_id bigint
)
DISTKEY(person_id);


CREATE TABLE #included_events

DISTKEY(person_id)
AS
WITH
cteIncludedEvents(event_id, person_id, start_date, end_date, op_start_date, op_end_date, ordinal) 
AS
(
  SELECT event_id, person_id, start_date, end_date, op_start_date, op_end_date, ROW_NUMBER() OVER (partition by person_id  ORDER BY start_date  ASC ) as ordinal
  from
  (
    select Q.event_id, Q.person_id, Q.start_date, Q.end_date, Q.op_start_date, Q.op_end_date, SUM(coalesce(POWER(cast(2 as bigint), I.inclusion_rule_id), 0)) as inclusion_rule_mask
    from #qualified_events Q
    LEFT JOIN #inclusionRuleCohorts I on I.person_id = Q.person_id and I.event_id = Q.event_id
    GROUP BY Q.event_id, Q.person_id, Q.start_date, Q.end_date, Q.op_start_date, Q.op_end_date
  ) MG -- matching groups

)

SELECT
event_id,  person_id , start_date, end_date, op_start_date, op_end_date

FROM
cteIncludedEvents Results
WHERE Results.ordinal = 1
;

-- Apply end date stratagies
-- by default, all events extend to the op_end_date.
CREATE TABLE #cohort_ends

DISTKEY(person_id)
AS
SELECT
event_id,  person_id , op_end_date as end_date

FROM
#included_events;





DELETE FROM public.arachne_cohorts where cohort_definition_id = 1;
INSERT INTO public.arachne_cohorts (cohort_definition_id, subject_id, cohort_start_date, cohort_end_date)
select 1 as cohort_definition_id, F.person_id, F.start_date, F.end_date
FROM (
  select I.person_id, I.start_date, E.end_date, ROW_NUMBER() OVER (partition by I.person_id, I.event_id  ORDER BY E.end_date ) as ordinal 
  from #included_events I
  join #cohort_ends E on I.event_id = E.event_id and I.person_id = E.person_id and E.end_date >= I.start_date
) F
WHERE F.ordinal = 1
;




TRUNCATE TABLE #cohort_ends;
DROP TABLE #cohort_ends;

TRUNCATE TABLE #inclusionRuleCohorts;
DROP TABLE #inclusionRuleCohorts;

TRUNCATE TABLE #qualified_events;
DROP TABLE #qualified_events;

TRUNCATE TABLE #included_events;
DROP TABLE #included_events;

TRUNCATE TABLE #Codesets;
DROP TABLE #Codesets