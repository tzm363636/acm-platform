-- Tighten nullable conditional metadata without rewriting V1 or deleting data.
ALTER TABLE submissions
 ADD CONSTRAINT chk_demo_scenario_required CHECK(data_kind!='DEMO' OR demo_scenario IS NOT NULL),
 ADD CONSTRAINT chk_fixture_metadata CHECK(origin!='fixture' OR demo_session_hash IS NULL),
 ADD CONSTRAINT chk_finished_order CHECK(finished_at IS NULL OR finished_at>=submitted_at),
 ADD CONSTRAINT chk_phase_verdict CHECK((phase IN ('waiting','compiling') AND verdict='Pending') OR (phase='judging' AND verdict='Judging') OR (phase='finished' AND verdict NOT IN ('Pending','Judging')));
ALTER TABLE submission_cases ADD CONSTRAINT chk_case_position CHECK(position>=0);
