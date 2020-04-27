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
 * Created: October 31, 2016
 *
 */

package com.odysseusinc.arachne.datanode;

import com.odysseusinc.arachne.commons.utils.ComparableVersion;

public interface Constants {


    String LOGIN_REGEX = "^[_'.@A-Za-z0-9-]*$";
    int PASSWORD_MIN_LENGTH = 6;
    int PASSWORD_MAX_LENGTH = 100;
    String DUMMY_PASSWORD = "password_was_set";
    String DUMMY_KEYFILE = "Replace current keyfile";

    interface AppConf {
        String PROFILE_DEVELOPMENT = "dev";
        String PROFILE_TEST = "test";
        String PROFILE_PRODUCTION = "prod";
    }

    interface Web {
        String PATTERN = "/**";
        String HOME = "/";
        String SWAGGER_UI = "/swagger-ui.html/**";
    }

    interface CentralApi {
        interface DataNode {
            String CREATION = "/api/v1/data-nodes";
            String UPDATE = "/api/v1/data-nodes/{uuid}";
            String BUILD_NUMBER = "/api/v1/build-number";
        }

        interface DataSource {
            String CREATION = "/api/v1/data-nodes/{id}/data-sources";
            String GET = "/api/v1/data-sources/{id}";
            String GET_LIST = "/api/v1/data-sources/commondata";
            String GET_METADATA_SCHEMA = "/api/v1/metadata/data-source/attributes";
            String UPDATE = "/api/v1/data-sources/{id}/from-node";
        }

        interface User {
            String AUTH_METHOD = "/api/v1/auth/method";
            String LOGIN = "/api/v1/auth/login";
            String PROFESSIONAL_TYPES = "/api/v1/user-management/professional-types";
            String COUNTRIES = "/api/v1/user-management/countries/search";
            String STATES_PROVINCES = "/api/v1/user-management/state-province/search";
            String REGISTER_USER = "/api/v1/auth/registration";
            String LOGOUT = "/api/v1/auth/logout";
            String REGISTRATION = "/api/v1/auth/registration";
            String SUGGEST = "/api/v1/user-management/users/suggest";
            String GET_USER = "/api/v1/user-management/users/byusername/{id}";
            String LINK_TO_NODE = "/api/v1/user-management/datanodes/{datanodeId}/users";
            String PASSWORD_POLICIES = "/api/v1/auth/password-policies";
        }

        interface Submission {
            String UPLOAD = "/api/v1/analysis-management/submissions/result/upload";
            String UPDATE_STATUS = "/api/v1/analysis-management/submissions/{id}/status/{password}";
            String DOWNLOAD_FILE = "/api/v1/analysis-management/submissions/{submissionId}/files?fileName={fileName}&updatePassword={updatePassword}";
        }

        interface Achilles {
            String LIST_REPORTS = "/api/v1/achilles/reports";
            String LIST_PERMISSIONS = "/api/v1/achilles/datanode/datasource/{id}/permissions";
            String PERMISSION = "/api/v1/achilles/datanode/datasource/{dataSourceId}/permissions/{id}";
            String PARAM_DATANODE = "datanode";
        }
    }

    interface Api {
        String PATTERN = "/api/**";

        interface User {
            String CENTRAL_REGISTER = "/centralregister";
        }

        interface Analysis {
            String INVALIDATE_ALL_UNFINISHED = "/api/v1/admin/analysis/invalidate";
        }

        interface Auth {
            String LOGIN_ENTRY_POINT = "/api/v1/auth/login/";
            String LOGOUT_ENTRY_POINT = "/api/v1/auth/logout/";
            String REGISTER_ENTRY_POINT = "/api/v1/auth/register/";
        }

        interface DataNode {
            String ADD = "/api/datanode/add";
            String CURRENT = "/api/datanode/current";
            String UPDATE = "/api/datanode/update";
            String CENTRAL_REGISTER = "/api/datanode/centralregister";
        }

        interface DataSource {
            String ADD = "/api/v1/data-sources";
            String ALL = "/api/v1/data-sources";
            String GET = "/api/v1/data-sources/{id}";
            String DELETE = "/api/v1/data-sources/{id}";
            String UPDATE = "/api/v1/data-sources/{id}";
            String CENTRAL_REGISTER = "/api/v1/data-sources/{id}/register-on-central";
            String DS_MODEL_CHECK_FIRSTCHECK ="/api/v1/data-sources/{id}/check/result/{password}/firstcheck";
            String DS_MODEL_CHECK_UPDATE = "/api/v1/data-sources/{id}/check/update/{password}";
            String GET_BUSINESS = "/api/v1/data-sources/{id}/business";
            String UPDATE_BUSINESS = "/api/v1/data-sources/{id}/business";
            String DELETE_KEYTAB = "/api/v1/data-sources/{id}/keytab";
        }
    }

    interface AnalysisMessages {
        String ANALYSIS_ALREADY_EXISTS_LOG = "Analysis with id='{}' is already exists, skipping";
        String ANALYSIS_IS_NOT_EXISTS_LOG = "Analysis with id='{}' is not exists";
        String CANT_REMOVE_RESULT_DIR_LOG = "Can't remove resultDir='{}' for analysis with id='{}'";
        String CANT_REMOVE_ANALYSIS_DIR_LOG = "Can't remove analysisDir='{}' for analysis with id='{}'";
        String RETRYING_DOWNLOAD_FILES_FOR_ANALYSIS_WITH_ID =
                "Retrying download files for analysis with id='{}'";
        String RESENDING_FAILURE_ANALYSIS_LOG =
                "Resending failure analysis with id='{}' to Execution Engine";
        String RESENDING_FAILURE_ANALYSIS_RESULT_LOG =
                "Resending failure analysis result with id='{}' to Central";
        String SENDING_STDOUT_TO_CENTRAL_LOG =
                "Sending stdout to central for submission with id='{}'";
        String SENDING_RESULT_TO_CENTRAL_LOG =
                "Sending result to central for submission with id='{}'";
        String UPDATE_STATUS_FAILED_LOG = "Update submission status id={} failed. Reason: {}";
        String STDOUT_UPDATED_REASON = "Stdout updated from Execution Engine";
        String ANALYSIS_FILES_READY_REASON =
                "Files for Analysis with id='%s' downloaded from central and unpacked";
        String ANALYSIS_FILES_DOWNLOAD_ERROR_REASON =
                "Download files for Analysis with id='%s' failure, reason=%s";
        String SEND_REQUEST_TO_ENGINE_SUCCESS_REASON = "Request with id=%s was sent to engine, status=%s";
        String SEND_REQUEST_TO_ENGINE_FAILED_REASON = "Sending request with id=%s failed, reason=%s";
        String SEND_ANALYSIS_RESULTS_TO_CENTRAL_SUCCESS_REASON =
                "Analysis results with id=%s was sent to central, AnalysisState=%s";
        String SEND_ANALYSIS_RESULT_TO_CENTRAL_FAILED_REASON =
                "Analysis results with id=%s sending failure, reason='%s', AnalysisState=%s";
        String SEND_ANALYSIS_RESULT_DTO_TO_CENTRAL_FAILED_REASON =
                "Analysis result dto with id=%s sending failure, reason='%s', AnalysisState=%s";
    }

    interface Analysis {
        String SUBMISSION_ARCHIVE_SUBDIR = "archive";
    }

    interface Achilles {

        String ACHILLES_SOURCE = "ACHILLES_SOURCE";
        String ACHILLES_DB_URI = "ACHILLES_DB_URI";
        String ACHILLES_CDM_SCHEMA = "ACHILLES_CDM_SCHEMA";
        String ACHILLES_VOCAB_SCHEMA = "ACHILLES_VOCAB_SCHEMA";
        String ACHILLES_RES_SCHEMA = "ACHILLES_RES_SCHEMA";
        String ACHILLES_CDM_VERSION = "ACHILLES_CDM_VERSION";
        String DEFAULT_CDM_VERSION = "5";
        String ACHILLES_NUM_THREADS = "ACHILLES_NUM_THREADS";
    }

    interface Atlas {

        String INFO = "/info";
        String LOGIN_DB = "/user/login/db";
        String LOGIN_LDAP = "/user/login/ldap";
        String COHORT_DEFINITION = "/cohortdefinition";

        ComparableVersion ATLAS_2_7_VERSION = new ComparableVersion("2.7.0");
    }

    interface CDM {
        String category = "category";
        String concept_id = "concept_id";
        String CONCEPT_ID = "CONCEPT_ID";
        String concept_name = "concept_name";
        String count_value = "count_value";
        String DRUG_CONCEPT_ID = "DRUG_CONCEPT_ID";
        String max_value = "max_value";
        String measurement_concept_id = "measurement_concept_id";
        String MEASUREMENT_CONCEPT_ID = "MEASUREMENT_CONCEPT_ID";
        String median_value = "median_value";
        String min_value = "min_value";
        String p10_value = "p10_value";
        String p25_value = "p25_value";
        String p75_value = "p75_value";
        String p90_value = "p90_value";
    }

    interface DataSourceMessages {
        String CANNOT_CREATE_DATASOURCE = "Unable to create data source on central.";
        String CANNOT_UPDATE_DATASOURCE = "Unable to update data source on central.";
        String DATASOURCE_NAME_UNIQUE = "Data source name is not unique";
    }

}
