/**
 * Author:  benoitmarsot
 * Created: Mar 30, 2023
 */
-- ALTER SEQUENCE assessment_assessmentid_seq RESTART WITH 1;
-- ALTER SEQUENCE assessmentversion_assessmentversionid_seq RESTART WITH 1;
-- ALTER SEQUENCE patient_patientid_seq RESTART WITH 1;
-- ALTER SEQUENCE provider_providerid_seq RESTART WITH 1;
ALTER SEQUENCE core.user_id_seq RESTART WITH 1;

drop table core.user;
drop table core.oaifile;
drop table core.vectorstore;
drop table core.vectorstore_oaifile;


select