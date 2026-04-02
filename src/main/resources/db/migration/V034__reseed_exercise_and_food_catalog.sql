-- V034: Re-seed exercise_catalog and food_catalog
-- Purpose: Restore catalog data lost/wiped on Render (both tables empty).
-- Idempotent: each INSERT uses WHERE NOT EXISTS so re-running is safe.
-- Sources: V018 (original 37 exercises) + V022 (99 new exercises) + V023 (GIF URLs) + V019 (foods)

-- ============================================================
-- EXERCISE CATALOG (~136 exercises)
-- ============================================================

-- ----------------------------------------
-- ORIGINAL V018 EXERCISES (with V022 GIF URLs baked in)
-- ----------------------------------------

-- CHEST
INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Bench Press', 'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{BARBELL,BENCH}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/EIeI8Vf.gif', 'https://static.exercisedb.dev/media/EIeI8Vf.gif', 'Lie flat on a bench, grip the bar slightly wider than shoulder width, lower to chest and press up.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Bench Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Incline Dumbbell Press', 'STRENGTH', '{CHEST,SHOULDERS}', '{DUMBBELL,BENCH}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/JHBRZa9.gif', 'https://static.exercisedb.dev/media/JHBRZa9.gif', 'Set bench to 30-45 degrees incline, press dumbbells from shoulder level to full extension above chest.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Incline Dumbbell Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Fly', 'STRENGTH', '{CHEST}', '{DUMBBELL,BENCH}', 'BEGINNER', 'https://static.exercisedb.dev/media/q0bTaXl.gif', 'https://static.exercisedb.dev/media/q0bTaXl.gif', 'Lie flat on a bench, hold dumbbells above chest with slight elbow bend, lower arms out wide then bring back together.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Fly' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Fly', 'STRENGTH', '{CHEST}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/MYkFWFm.gif', 'https://static.exercisedb.dev/media/MYkFWFm.gif', 'Stand between cable machine pulleys set at shoulder height, pull handles together in front of chest in a wide arc.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Fly' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Push Up', 'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/Wk7BRHE.gif', 'https://static.exercisedb.dev/media/Wk7BRHE.gif', 'Start in plank position with hands shoulder-width apart, lower chest to ground then push back up.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dip', 'STRENGTH', '{CHEST,TRICEPS}', '{BODYWEIGHT,DIP_BAR}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/kh4mNBP.gif', 'https://static.exercisedb.dev/media/kh4mNBP.gif', 'Grip parallel bars, lower body by bending elbows until upper arms are parallel to floor, then press back up.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dip' AND is_system = TRUE);

-- LEGS
INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Back Squat', 'STRENGTH', '{LEGS,GLUTES,CORE}', '{BARBELL,SQUAT_RACK}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/XGhBMn0.gif', 'https://static.exercisedb.dev/media/XGhBMn0.gif', 'Bar rests on upper traps, feet shoulder-width apart, squat down until thighs are parallel to floor, drive back up.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Back Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Leg Press', 'STRENGTH', '{LEGS,GLUTES}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/GNeodj1.gif', 'https://static.exercisedb.dev/media/GNeodj1.gif', 'Sit in leg press machine, place feet shoulder-width on platform, lower weight by bending knees then press back.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Leg Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Romanian Deadlift', 'STRENGTH', '{HAMSTRINGS,GLUTES,BACK}', '{BARBELL}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/qa18JXi.gif', 'https://static.exercisedb.dev/media/qa18JXi.gif', 'Hold barbell at hip level, hinge at hips pushing them back while lowering bar along legs, return by driving hips forward.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Romanian Deadlift' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Leg Curl', 'STRENGTH', '{HAMSTRINGS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/feKk6ND.gif', 'https://static.exercisedb.dev/media/feKk6ND.gif', 'Lie face down on leg curl machine, curl heels toward glutes against resistance, then lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Leg Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Leg Extension', 'STRENGTH', '{QUADRICEPS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/y1pFQ3Y.gif', 'https://static.exercisedb.dev/media/y1pFQ3Y.gif', 'Sit in leg extension machine, extend legs to full lockout against resistance, then lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Leg Extension' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Calf Raise', 'STRENGTH', '{CALVES}', '{MACHINE,BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/2dkaBre.gif', 'https://static.exercisedb.dev/media/2dkaBre.gif', 'Stand with balls of feet on an elevated surface, raise heels as high as possible, then lower below platform level.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Calf Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Bulgarian Split Squat', 'STRENGTH', '{LEGS,GLUTES}', '{DUMBBELL,BENCH}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/eDMTsnp.gif', 'https://static.exercisedb.dev/media/eDMTsnp.gif', 'Rear foot elevated on bench, front foot forward, lower rear knee toward ground then drive back up through front heel.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Bulgarian Split Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Lunge', 'STRENGTH', '{LEGS,GLUTES}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/WGE82wh.gif', 'https://static.exercisedb.dev/media/WGE82wh.gif', 'Hold dumbbells at sides, step forward with one leg and lower back knee toward floor, return to standing and alternate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Lunge' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Hip Thrust', 'STRENGTH', '{GLUTES,HAMSTRINGS}', '{BARBELL,BENCH}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/NEbIO3s.gif', 'https://static.exercisedb.dev/media/NEbIO3s.gif', 'Shoulders on bench, barbell across hips, drive hips up by squeezing glutes until body is straight, lower and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Hip Thrust' AND is_system = TRUE);

-- BACK
INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Conventional Deadlift', 'STRENGTH', '{BACK,LEGS,CORE}', '{BARBELL}', 'ADVANCED', 'https://static.exercisedb.dev/media/xKvip1W.gif', 'https://static.exercisedb.dev/media/xKvip1W.gif', 'Grip bar shoulder-width, flat back, push floor away to stand with the bar, then hinge back down under control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Conventional Deadlift' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Pull Up', 'STRENGTH', '{BACK,BICEPS}', '{BODYWEIGHT,PULL_UP_BAR}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/hbIxky5.gif', 'https://static.exercisedb.dev/media/hbIxky5.gif', 'Hang from bar with overhand grip, pull chest to bar by driving elbows toward hips, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Pull Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Lat Pulldown', 'STRENGTH', '{BACK,BICEPS}', '{CABLE,MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/vJ5A2BN.gif', 'https://static.exercisedb.dev/media/vJ5A2BN.gif', 'Grip wide bar overhead at cable machine, pull bar to upper chest while leaning slightly back, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Lat Pulldown' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Row', 'STRENGTH', '{BACK,BICEPS}', '{BARBELL}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/Jg0K4fw.gif', 'https://static.exercisedb.dev/media/Jg0K4fw.gif', 'Hip-hinge to 45 degrees, grip barbell, row to lower chest while keeping elbows close to torso, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Seated Cable Row', 'STRENGTH', '{BACK,BICEPS}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/rpRWrO4.gif', 'https://static.exercisedb.dev/media/rpRWrO4.gif', 'Sit at cable row machine, pull handle to abdomen while driving elbows back, squeeze shoulder blades, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Seated Cable Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Face Pull', 'STRENGTH', '{SHOULDERS,BACK}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/KKqAVfD.gif', 'https://static.exercisedb.dev/media/KKqAVfD.gif', 'Set cable at face height with rope attachment, pull toward face with elbows flared high, externally rotate at end.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Face Pull' AND is_system = TRUE);

-- SHOULDERS
INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Overhead Press', 'STRENGTH', '{SHOULDERS,TRICEPS}', '{BARBELL,SQUAT_RACK}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/cGVPgvN.gif', 'https://static.exercisedb.dev/media/cGVPgvN.gif', 'Bar at collar bone height, press overhead to full lockout, lower back to starting position under control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Overhead Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Shoulder Press', 'STRENGTH', '{SHOULDERS,TRICEPS}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/RJHAQJD.gif', 'https://static.exercisedb.dev/media/RJHAQJD.gif', 'Sit or stand, hold dumbbells at shoulder height, press overhead to full extension then lower back down.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Shoulder Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Lateral Raise', 'STRENGTH', '{SHOULDERS}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/Wfwcmcr.gif', 'https://static.exercisedb.dev/media/Wfwcmcr.gif', 'Stand with dumbbells at sides, raise arms out to shoulder height with slight elbow bend, lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Lateral Raise' AND is_system = TRUE);

-- ARMS
INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Curl', 'STRENGTH', '{BICEPS}', '{BARBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/JBvfSNc.gif', 'https://static.exercisedb.dev/media/JBvfSNc.gif', 'Stand with barbell at thigh level, curl to shoulder height by bending elbows, lower under control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Hammer Curl', 'STRENGTH', '{BICEPS,FOREARMS}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/X14FNBK.gif', 'https://static.exercisedb.dev/media/X14FNBK.gif', 'Hold dumbbells with neutral grip (palms facing each other), curl to shoulder height, lower under control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Hammer Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Tricep Pushdown', 'STRENGTH', '{TRICEPS}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/IOz5ioq.gif', 'https://static.exercisedb.dev/media/IOz5ioq.gif', 'Stand at cable machine with bar at upper pulley, push bar down to full extension keeping elbows at sides, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Tricep Pushdown' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Overhead Tricep Extension', 'STRENGTH', '{TRICEPS}', '{DUMBBELL,CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/MnMDBek.gif', 'https://static.exercisedb.dev/media/MnMDBek.gif', 'Hold weight overhead with both hands, lower behind head by bending elbows, extend back to start.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Overhead Tricep Extension' AND is_system = TRUE);

-- CORE
INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Plank', 'STRENGTH', '{CORE}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/ooAuqIB.gif', 'https://static.exercisedb.dev/media/ooAuqIB.gif', 'Hold a push-up position on forearms and toes, keep body straight from head to heels, breathe steadily.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Plank' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Russian Twist', 'STRENGTH', '{CORE}', '{BODYWEIGHT,DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/XVDdcoj.gif', 'https://static.exercisedb.dev/media/XVDdcoj.gif', 'Sit with knees bent and feet off floor, lean back slightly, rotate torso side to side touching floor or holding weight.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Russian Twist' AND is_system = TRUE);

-- CARDIO (V018 originals)
INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Treadmill Run', 'CARDIO', '{CARDIO}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/KCPHUoD.gif', 'https://static.exercisedb.dev/media/KCPHUoD.gif', 'Set treadmill to desired speed and incline, maintain upright posture and consistent cadence throughout session.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Treadmill Run' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Stationary Bike', 'CARDIO', '{CARDIO,LEGS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/DO3MuGk.gif', 'https://static.exercisedb.dev/media/DO3MuGk.gif', 'Adjust seat height so knee has slight bend at bottom of pedal stroke, maintain steady pace at target heart rate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Stationary Bike' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Rowing Machine', 'CARDIO', '{CARDIO,BACK,ARMS}', '{MACHINE}', 'INTERMEDIATE', NULL, NULL, 'Drive with legs first, then lean back and pull handle to lower chest, return by reversing the sequence.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Rowing Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Jump Rope', 'CARDIO', '{CARDIO,CALVES}', '{JUMP_ROPE}', 'BEGINNER', 'https://static.exercisedb.dev/media/hiOcEEk.gif', 'https://static.exercisedb.dev/media/hiOcEEk.gif', 'Keep elbows close to body, rotate wrists to swing rope, jump lightly on balls of feet with each pass.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Jump Rope' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Stair Climber', 'CARDIO', '{CARDIO,LEGS,GLUTES}', '{MACHINE}', 'INTERMEDIATE', NULL, NULL, 'Maintain upright posture with slight forward lean, step fully onto each stair, avoid holding handrails for full benefit.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Stair Climber' AND is_system = TRUE);

-- FLEXIBILITY
INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Yoga Flow', 'FLEXIBILITY', '{FLEXIBILITY,CORE}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Move through a series of linked yoga poses coordinating breath with movement to improve flexibility and mindfulness.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Yoga Flow' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Static Stretching', 'FLEXIBILITY', '{FLEXIBILITY}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Hold each stretch position for 20-30 seconds without bouncing, focusing on major muscle groups after exercise.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Static Stretching' AND is_system = TRUE);

-- ----------------------------------------
-- V022 NEW EXERCISES: BODYWEIGHT (~28)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Mountain Climber', 'STRENGTH', '{CORE,SHOULDERS,LEGS}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/RJgzwny.gif', 'https://static.exercisedb.dev/media/RJgzwny.gif', 'Start in a high plank, alternate driving knees toward chest in a running motion while keeping hips level. Move as fast as possible while maintaining form.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Mountain Climber' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Burpee', 'CARDIO', '{CARDIO,CHEST,LEGS,CORE}', '{BODYWEIGHT}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/dK9394r.gif', 'https://static.exercisedb.dev/media/dK9394r.gif', 'From standing, squat down and place hands on floor, jump feet back to plank, perform a push-up, jump feet back to hands, then explosively jump up with arms overhead.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Burpee' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Jumping Jack', 'CARDIO', '{CARDIO,SHOULDERS,LEGS}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/1g5bPpA.gif', 'https://static.exercisedb.dev/media/1g5bPpA.gif', 'Start standing with feet together and arms at sides, jump feet wide while raising arms overhead, then return to start in one fluid motion.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Jumping Jack' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Box Jump', 'STRENGTH', '{LEGS,GLUTES,CALVES}', '{BODYWEIGHT,PLYO_BOX}', 'INTERMEDIATE', NULL, NULL, 'Stand facing a sturdy box, bend knees and swing arms to load the jump, explode upward landing softly on both feet on the box, then step down.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Box Jump' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Bear Crawl', 'STRENGTH', '{CORE,SHOULDERS,LEGS}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/0Yz8WdV.gif', 'https://static.exercisedb.dev/media/0Yz8WdV.gif', 'Start on all fours with knees hovering one inch off the ground, move opposite hand and foot forward simultaneously while keeping hips low and core braced.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Bear Crawl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Pike Push Up', 'STRENGTH', '{SHOULDERS,TRICEPS}', '{BODYWEIGHT}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/sVvXT5J.gif', 'https://static.exercisedb.dev/media/sVvXT5J.gif', 'Form an inverted V with hips high and hands and feet on the floor, bend elbows to lower your head toward the ground, then push back up.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Pike Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Diamond Push Up', 'STRENGTH', '{TRICEPS,CHEST}', '{BODYWEIGHT}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/soIB2rj.gif', 'https://static.exercisedb.dev/media/soIB2rj.gif', 'Form a diamond shape with index fingers and thumbs on the ground, perform a push-up keeping elbows close to the body throughout the movement.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Diamond Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Wide Push Up', 'STRENGTH', '{CHEST,SHOULDERS}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/JmMVpR3.gif', 'https://static.exercisedb.dev/media/JmMVpR3.gif', 'Place hands wider than shoulder-width apart, lower chest toward the floor while keeping body straight, press back up to starting position.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Wide Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Decline Push Up', 'STRENGTH', '{CHEST,SHOULDERS,TRICEPS}', '{BODYWEIGHT,BENCH}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/i5cEhka.gif', 'https://static.exercisedb.dev/media/i5cEhka.gif', 'Elevate feet on a bench or box with hands on the floor, perform a push-up targeting the upper chest, keeping core tight throughout.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Decline Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Archer Push Up', 'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{BODYWEIGHT}', 'ADVANCED', 'https://static.exercisedb.dev/media/A9qxk2F.gif', 'https://static.exercisedb.dev/media/A9qxk2F.gif', 'Start in wide push-up position, lower to one side while extending the opposite arm straight, alternate sides each rep to build unilateral strength.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Archer Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Pistol Squat', 'STRENGTH', '{LEGS,GLUTES,CORE}', '{BODYWEIGHT}', 'ADVANCED', 'https://static.exercisedb.dev/media/5bpPTHv.gif', 'https://static.exercisedb.dev/media/5bpPTHv.gif', 'Stand on one leg with the other extended forward, lower into a full squat on the standing leg while keeping the extended leg off the ground, then press back up.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Pistol Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Jump Squat', 'STRENGTH', '{LEGS,GLUTES,CALVES}', '{BODYWEIGHT}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/LIlE5Tn.gif', 'https://static.exercisedb.dev/media/LIlE5Tn.gif', 'Perform a standard squat, then explosively jump off the ground at the top, land softly with bent knees and immediately descend into the next rep.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Jump Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Wall Sit', 'STRENGTH', '{QUADRICEPS,GLUTES}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/sVQCCeG.gif', 'https://static.exercisedb.dev/media/sVQCCeG.gif', 'Slide back down a wall until thighs are parallel to the floor and knees are at 90 degrees, hold position with back flat against the wall.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Wall Sit' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Glute Bridge', 'STRENGTH', '{GLUTES,HAMSTRINGS,CORE}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/qKBpF7I.gif', 'https://static.exercisedb.dev/media/qKBpF7I.gif', 'Lie on back with knees bent and feet flat, drive hips upward by squeezing glutes until body forms a straight line from shoulders to knees, lower and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Glute Bridge' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Single Leg Deadlift', 'STRENGTH', '{HAMSTRINGS,GLUTES,BACK}', '{BODYWEIGHT}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/daBmy1Y.gif', 'https://static.exercisedb.dev/media/daBmy1Y.gif', 'Stand on one leg, hinge forward at the hip while extending the free leg behind you, keep back flat until feeling a hamstring stretch, then return upright.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Single Leg Deadlift' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Inverted Row', 'STRENGTH', '{BACK,BICEPS}', '{BODYWEIGHT,BARBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/bZGHsAZ.gif', 'https://static.exercisedb.dev/media/bZGHsAZ.gif', 'Hang beneath a barbell or rings with arms extended and body straight, pull chest up to the bar by driving elbows back, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Inverted Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Chin Up', 'STRENGTH', '{BACK,BICEPS}', '{BODYWEIGHT,PULL_UP_BAR}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/T2mxWqc.gif', 'https://static.exercisedb.dev/media/T2mxWqc.gif', 'Grip a bar with palms facing you at shoulder width, pull chin above the bar by driving elbows down and squeezing biceps, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Chin Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Hanging Leg Raise', 'STRENGTH', '{CORE,HIP_FLEXORS}', '{BODYWEIGHT,PULL_UP_BAR}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/I3tsCnC.gif', 'https://static.exercisedb.dev/media/I3tsCnC.gif', 'Hang from a pull-up bar with arms extended, brace core and raise straight legs to hip height or above, lower slowly without swinging.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Hanging Leg Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'L-Sit', 'STRENGTH', '{CORE,HIP_FLEXORS,TRICEPS}', '{BODYWEIGHT}', 'ADVANCED', 'https://static.exercisedb.dev/media/UpWmA5E.gif', 'https://static.exercisedb.dev/media/UpWmA5E.gif', 'Support your body on two parallel bars or on the floor, extend legs straight out in front forming an L shape, hold while keeping hips from dropping.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'L-Sit' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Flutter Kick', 'STRENGTH', '{CORE,HIP_FLEXORS}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/UVo2Qs2.gif', 'https://static.exercisedb.dev/media/UVo2Qs2.gif', 'Lie on your back with legs extended and slightly raised, alternate kicking legs up and down in a small controlled motion while keeping the lower back pressed down.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Flutter Kick' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Bicycle Crunch', 'STRENGTH', '{CORE}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/tZkGYZ9.gif', 'https://static.exercisedb.dev/media/tZkGYZ9.gif', 'Lie on back with hands behind head, bring opposite elbow and knee together while extending the other leg, alternate sides in a pedaling motion.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Bicycle Crunch' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dead Bug', 'STRENGTH', '{CORE}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/iny3m5y.gif', 'https://static.exercisedb.dev/media/iny3m5y.gif', 'Lie on back with arms extended to the ceiling and knees bent at 90 degrees, slowly lower opposite arm and leg toward the floor while keeping lower back pressed down, return and alternate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dead Bug' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Bird Dog', 'STRENGTH', '{CORE,BACK,GLUTES}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Start on all fours with a neutral spine, extend the opposite arm and leg simultaneously while keeping hips level, hold briefly then alternate sides.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Bird Dog' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Superman', 'STRENGTH', '{BACK,GLUTES}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/zkgRrbK.gif', 'https://static.exercisedb.dev/media/zkgRrbK.gif', 'Lie face down with arms extended overhead, simultaneously lift arms, chest, and legs off the floor by squeezing glutes and back muscles, hold briefly then lower.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Superman' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Crab Walk', 'STRENGTH', '{CORE,TRICEPS,GLUTES}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Sit on the floor with hands behind and feet flat, lift hips off the ground, then walk forward or backward by moving opposite hand and foot simultaneously.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Crab Walk' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'High Knees', 'CARDIO', '{CARDIO,LEGS,CORE}', '{BODYWEIGHT}', 'BEGINNER', 'https://static.exercisedb.dev/media/ealLwvX.gif', 'https://static.exercisedb.dev/media/ealLwvX.gif', 'Run in place driving knees up to hip height alternately, pump arms in opposition, maintain a fast cadence to elevate heart rate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'High Knees' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Butt Kicks', 'CARDIO', '{CARDIO,HAMSTRINGS}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Run in place kicking heels up toward your glutes with each stride, keep torso upright and arms pumping, maintain a quick rhythm.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Butt Kicks' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Tuck Jump', 'STRENGTH', '{LEGS,GLUTES,CORE}', '{BODYWEIGHT}', 'INTERMEDIATE', NULL, NULL, 'Stand with feet shoulder-width apart, bend knees slightly and jump explosively, pulling both knees to chest at the peak, land softly and immediately repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Tuck Jump' AND is_system = TRUE);

-- ----------------------------------------
-- V022 NEW EXERCISES: DUMBBELL (~20)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Bench Press', 'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{DUMBBELL,BENCH}', 'BEGINNER', 'https://static.exercisedb.dev/media/SpYC0Kp.gif', 'https://static.exercisedb.dev/media/SpYC0Kp.gif', 'Lie flat on a bench holding dumbbells at chest level, press both up to full extension above the chest, lower slowly to starting position.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Bench Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Row', 'STRENGTH', '{BACK,BICEPS}', '{DUMBBELL,BENCH}', 'BEGINNER', 'https://static.exercisedb.dev/media/BJ0Hz5L.gif', 'https://static.exercisedb.dev/media/BJ0Hz5L.gif', 'Place one hand and knee on a bench for support, hold a dumbbell in the opposite hand, row it to your hip by driving the elbow back, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Romanian Deadlift', 'STRENGTH', '{HAMSTRINGS,GLUTES,BACK}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/rR0LJzx.gif', 'https://static.exercisedb.dev/media/rR0LJzx.gif', 'Hold dumbbells in front of thighs, hinge at hips pushing them back while lowering dumbbells along the legs, keep back flat and return by driving hips forward.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Romanian Deadlift' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Goblet Squat', 'STRENGTH', '{LEGS,GLUTES,CORE}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/yn8yg1r.gif', 'https://static.exercisedb.dev/media/yn8yg1r.gif', 'Hold a dumbbell vertically at chest level, squat down keeping elbows inside knees at the bottom, drive through heels to return to standing.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Goblet Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Step Up', 'STRENGTH', '{LEGS,GLUTES}', '{DUMBBELL,BENCH}', 'BEGINNER', 'https://static.exercisedb.dev/media/aXtJhlg.gif', 'https://static.exercisedb.dev/media/aXtJhlg.gif', 'Hold dumbbells at sides, step one foot onto a bench, drive through that heel to lift the body up, step back down and alternate legs.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Step Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Pullover', 'STRENGTH', '{CHEST,BACK,TRICEPS}', '{DUMBBELL,BENCH}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/9XjtHvS.gif', 'https://static.exercisedb.dev/media/9XjtHvS.gif', 'Lie on a bench holding one dumbbell with both hands over your chest, lower the weight in an arc behind your head until you feel a chest stretch, pull back to start.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Pullover' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Reverse Fly', 'STRENGTH', '{BACK,SHOULDERS}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/EAs3xL9.gif', 'https://static.exercisedb.dev/media/EAs3xL9.gif', 'Hinge forward at the hips holding dumbbells, raise arms out to the sides with a slight elbow bend until level with your back, squeeze shoulder blades and lower.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Reverse Fly' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Front Raise', 'STRENGTH', '{SHOULDERS}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/3eGE2JC.gif', 'https://static.exercisedb.dev/media/3eGE2JC.gif', 'Stand holding dumbbells at thigh level, raise both arms straight out in front to shoulder height, lower slowly and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Front Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Shrug', 'STRENGTH', '{TRAPS}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/NJzBsGJ.gif', 'https://static.exercisedb.dev/media/NJzBsGJ.gif', 'Hold dumbbells at sides with arms straight, elevate shoulders straight up toward ears as high as possible, hold briefly then lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Shrug' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Concentration Curl', 'STRENGTH', '{BICEPS}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/gvsWLQw.gif', 'https://static.exercisedb.dev/media/gvsWLQw.gif', 'Sit on a bench, rest the back of one upper arm on the inner thigh, curl the dumbbell to shoulder height focusing on the bicep peak, lower fully.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Concentration Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Skull Crusher', 'STRENGTH', '{TRICEPS}', '{DUMBBELL,BENCH}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/mpKZGWz.gif', 'https://static.exercisedb.dev/media/mpKZGWz.gif', 'Lie on a bench holding dumbbells above chest, lower them toward your forehead by bending only the elbows, extend back to the start keeping upper arms stationary.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Skull Crusher' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Kickback', 'STRENGTH', '{TRICEPS}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/W6PxUkg.gif', 'https://static.exercisedb.dev/media/W6PxUkg.gif', 'Hinge forward with upper arm parallel to the floor, extend the forearm back to full lockout by straightening the elbow, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Kickback' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Calf Raise', 'STRENGTH', '{CALVES}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/dPmaUaU.gif', 'https://static.exercisedb.dev/media/dPmaUaU.gif', 'Stand holding dumbbells at your sides with feet hip-width apart, raise onto the balls of your feet as high as possible, lower heels below the starting point for a full stretch.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Calf Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Arnold Press', 'STRENGTH', '{SHOULDERS,TRICEPS}', '{DUMBBELL}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/Xy4jlWA.gif', 'https://static.exercisedb.dev/media/Xy4jlWA.gif', 'Hold dumbbells at chin level with palms facing you, rotate palms outward while pressing overhead to full extension, reverse the rotation on the way down.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Arnold Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Wrist Curl', 'STRENGTH', '{FOREARMS}', '{DUMBBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/YtaCTYl.gif', 'https://static.exercisedb.dev/media/YtaCTYl.gif', 'Sit with forearms resting on thighs and palms facing up holding dumbbells, curl wrists upward by flexing the forearms, lower fully and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Wrist Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Swing', 'STRENGTH', '{GLUTES,HAMSTRINGS,CORE,SHOULDERS}', '{DUMBBELL}', 'INTERMEDIATE', NULL, NULL, 'Hold one dumbbell with both hands, hinge at the hips to swing it back between the legs, then drive hips forward explosively to swing it to chest height.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Swing' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Floor Press', 'STRENGTH', '{CHEST,TRICEPS}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Lie on the floor holding dumbbells at chest level, press them to full extension above the chest, lower until elbows touch the floor and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Floor Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Lateral Lunge', 'STRENGTH', '{LEGS,GLUTES,ADDUCTORS}', '{DUMBBELL}', 'INTERMEDIATE', NULL, NULL, 'Hold dumbbells at sides, step one foot wide to the side and bend that knee while keeping the opposite leg straight, push back to center and alternate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Lateral Lunge' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Sumo Squat', 'STRENGTH', '{LEGS,GLUTES,ADDUCTORS}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Stand with feet wider than shoulder-width and toes turned out, hold one dumbbell between the legs, squat down keeping chest tall, drive through heels to stand.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Sumo Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Snatch', 'STRENGTH', '{SHOULDERS,BACK,LEGS,CORE}', '{DUMBBELL}', 'ADVANCED', 'https://static.exercisedb.dev/media/6pTkI99.gif', 'https://static.exercisedb.dev/media/6pTkI99.gif', 'Start with dumbbell on the floor, pull it explosively from the ground to overhead in one fluid movement by extending hips, knees, and ankles while keeping it close to the body.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Snatch' AND is_system = TRUE);

-- ----------------------------------------
-- V022 NEW EXERCISES: BARBELL (~13)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Hip Thrust', 'STRENGTH', '{GLUTES,HAMSTRINGS}', '{BARBELL,BENCH}', 'INTERMEDIATE', NULL, NULL, 'Sit with upper back against a bench, barbell across hips, drive hips upward by squeezing glutes until body forms a straight line, lower and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Hip Thrust' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Shrug', 'STRENGTH', '{TRAPS}', '{BARBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/dG7tG5y.gif', 'https://static.exercisedb.dev/media/dG7tG5y.gif', 'Hold a barbell in front of thighs with arms straight, elevate shoulders straight up toward ears as high as possible, hold briefly then lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Shrug' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Front Squat', 'STRENGTH', '{LEGS,CORE,SHOULDERS}', '{BARBELL,SQUAT_RACK}', 'ADVANCED', 'https://static.exercisedb.dev/media/zG0zs85.gif', 'https://static.exercisedb.dev/media/zG0zs85.gif', 'Rest the barbell on the front deltoids with elbows high, squat down keeping an upright torso until thighs are parallel, drive back up to full extension.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Front Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Lunge', 'STRENGTH', '{LEGS,GLUTES}', '{BARBELL,SQUAT_RACK}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/t8iSghb.gif', 'https://static.exercisedb.dev/media/t8iSghb.gif', 'With a barbell on your upper back, step forward into a lunge lowering the rear knee toward the floor, push through the front heel to return to standing, alternate legs.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Lunge' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Good Morning', 'STRENGTH', '{HAMSTRINGS,GLUTES,BACK}', '{BARBELL}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/XlZ4lAC.gif', 'https://static.exercisedb.dev/media/XlZ4lAC.gif', 'Place a barbell on your upper traps, hinge forward at the hips with a slight knee bend until torso is near parallel to the floor, drive hips forward to return upright.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Good Morning' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Sumo Deadlift', 'STRENGTH', '{LEGS,GLUTES,BACK,CORE}', '{BARBELL}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/KgI0tqW.gif', 'https://static.exercisedb.dev/media/KgI0tqW.gif', 'Stand with feet wide and toes turned out, grip bar inside the knees, keep chest tall and push the floor away to stand, keeping bar close to the body throughout.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Sumo Deadlift' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Close Grip Bench Press', 'STRENGTH', '{TRICEPS,CHEST}', '{BARBELL,BENCH}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/J6Dx1Mu.gif', 'https://static.exercisedb.dev/media/J6Dx1Mu.gif', 'Lie on a bench and grip the barbell with hands shoulder-width apart, lower to the lower chest keeping elbows close to the torso, press back to full extension.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Close Grip Bench Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Upright Row', 'STRENGTH', '{SHOULDERS,TRAPS}', '{BARBELL}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/UDlhcO8.gif', 'https://static.exercisedb.dev/media/UDlhcO8.gif', 'Hold a barbell at hip level with an overhand grip, pull it straight up along the body to chin height leading with the elbows, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Upright Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Reverse Curl', 'STRENGTH', '{BICEPS,FOREARMS}', '{BARBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/xNrS20v.gif', 'https://static.exercisedb.dev/media/xNrS20v.gif', 'Hold a barbell with an overhand grip at thigh level, curl to shoulder height keeping wrists neutral, lower under control emphasizing the brachialis.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Reverse Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Pendlay Row', 'STRENGTH', '{BACK,BICEPS}', '{BARBELL}', 'ADVANCED', 'https://static.exercisedb.dev/media/r0z6xzQ.gif', 'https://static.exercisedb.dev/media/r0z6xzQ.gif', 'Start with bar on the floor, torso parallel to the ground, explosively pull the bar to the lower chest and return it fully to the floor each rep, maintaining a strict horizontal torso.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Pendlay Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'T-Bar Row', 'STRENGTH', '{BACK,BICEPS}', '{BARBELL,MACHINE}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/aaXr7ld.gif', 'https://static.exercisedb.dev/media/aaXr7ld.gif', 'Straddle a landmine barbell or use a T-bar row machine, hinge forward and row the handles to the lower chest, squeeze shoulder blades at the top, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'T-Bar Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Zercher Squat', 'STRENGTH', '{LEGS,CORE,BACK}', '{BARBELL}', 'ADVANCED', 'https://static.exercisedb.dev/media/LSTChY9.gif', 'https://static.exercisedb.dev/media/LSTChY9.gif', 'Hold the barbell in the crooks of your elbows at waist height, squat down keeping an upright torso, drive through heels to return to standing.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Zercher Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Wrist Curl', 'STRENGTH', '{FOREARMS}', '{BARBELL}', 'BEGINNER', 'https://static.exercisedb.dev/media/82LxxkW.gif', 'https://static.exercisedb.dev/media/82LxxkW.gif', 'Sit with forearms resting on thighs and palms up holding a barbell, curl wrists upward by flexing the forearms fully, lower completely and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Wrist Curl' AND is_system = TRUE);

-- ----------------------------------------
-- V022 NEW EXERCISES: CABLE (~12)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Lateral Raise', 'STRENGTH', '{SHOULDERS}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/goJ6ezq.gif', 'https://static.exercisedb.dev/media/goJ6ezq.gif', 'Stand beside a low cable pulley with the handle in the far hand, raise the arm out to shoulder height with a slight elbow bend, lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Lateral Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Curl', 'STRENGTH', '{BICEPS}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/G08RZcQ.gif', 'https://static.exercisedb.dev/media/G08RZcQ.gif', 'Stand at a low cable pulley, grip the bar or handle with an underhand grip, curl to shoulder height keeping elbows at sides, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Tricep Extension', 'STRENGTH', '{TRICEPS}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/3ZflifB.gif', 'https://static.exercisedb.dev/media/3ZflifB.gif', 'Stand at a high pulley with a rope or bar attachment, keep upper arms at sides and extend forearms down to full lockout, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Tricep Extension' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Woodchop', 'STRENGTH', '{CORE,SHOULDERS}', '{CABLE}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/aVs3BR3.gif', 'https://static.exercisedb.dev/media/aVs3BR3.gif', 'Set cable at shoulder height, grip the handle with both hands, rotate and pull in a diagonal downward arc across the body, control the return.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Woodchop' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Pull Through', 'STRENGTH', '{GLUTES,HAMSTRINGS}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/OM46QHm.gif', 'https://static.exercisedb.dev/media/OM46QHm.gif', 'Face away from a low cable with the rope between your legs, hinge forward at the hips until torso is near parallel, drive hips forward to stand tall and squeeze glutes.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Pull Through' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Crunch', 'STRENGTH', '{CORE}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/8xUv4J7.gif', 'https://static.exercisedb.dev/media/8xUv4J7.gif', 'Kneel at a high pulley holding the rope at your forehead, crunch downward by contracting the abs until elbows meet the knees, return under control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Crunch' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Crossover', 'STRENGTH', '{CHEST}', '{CABLE}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/0CXGHya.gif', 'https://static.exercisedb.dev/media/0CXGHya.gif', 'Stand between two high cable pulleys, pull the handles downward and together in a wide arc in front of your chest, squeeze at the midpoint then return.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Crossover' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Kickback', 'STRENGTH', '{GLUTES}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/HEJ6DIX.gif', 'https://static.exercisedb.dev/media/HEJ6DIX.gif', 'Attach an ankle strap to a low cable, face the machine and hinge slightly forward, kick the attached leg straight back squeezing the glute at the top, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Kickback' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Upright Row', 'STRENGTH', '{SHOULDERS,TRAPS}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/cALKspW.gif', 'https://static.exercisedb.dev/media/cALKspW.gif', 'Stand at a low cable pulley, pull the bar straight up along the body to chin height leading with the elbows flared wide, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Upright Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Reverse Fly', 'STRENGTH', '{BACK,SHOULDERS}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/yUdIGNs.gif', 'https://static.exercisedb.dev/media/yUdIGNs.gif', 'Stand between two low cable pulleys with crossed handles, pull arms apart in a wide arc to shoulder height squeezing the rear delts, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Reverse Fly' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Front Raise', 'STRENGTH', '{SHOULDERS}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/u2X71Np.gif', 'https://static.exercisedb.dev/media/u2X71Np.gif', 'Stand at a low cable pulley with the handle in front, raise both arms straight forward to shoulder height, lower slowly and with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Front Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Hammer Curl', 'STRENGTH', '{BICEPS,FOREARMS}', '{CABLE}', 'BEGINNER', 'https://static.exercisedb.dev/media/HPlPoQA.gif', 'https://static.exercisedb.dev/media/HPlPoQA.gif', 'Attach a rope to a low cable, grip with a neutral (hammer) grip, curl rope to shoulder height keeping elbows at sides, lower with full control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Hammer Curl' AND is_system = TRUE);

-- ----------------------------------------
-- V022 NEW EXERCISES: MACHINE (~13)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Smith Machine Squat', 'STRENGTH', '{LEGS,GLUTES}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/jFtipLl.gif', 'https://static.exercisedb.dev/media/jFtipLl.gif', 'Position the bar on the Smith machine across your upper traps, unhook and squat to parallel keeping knees tracking over toes, press back up and re-rack.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Smith Machine Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Hack Squat', 'STRENGTH', '{LEGS,GLUTES}', '{MACHINE}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/Qa55kX1.gif', 'https://static.exercisedb.dev/media/Qa55kX1.gif', 'Position yourself in the hack squat machine with shoulders and back against the pads, lower until thighs are at or below parallel, press back up without locking out knees.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Hack Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Chest Press Machine', 'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/DOoWcnA.gif', 'https://static.exercisedb.dev/media/DOoWcnA.gif', 'Sit in the chest press machine with back flat against the pad, grip the handles at chest level and press to full extension, return slowly to the start.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Chest Press Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Pec Deck', 'STRENGTH', '{CHEST}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Sit in the pec deck machine with elbows on the arm pads, bring the pads together in front of your chest squeezing the pecs, return with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Pec Deck' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Shoulder Press Machine', 'STRENGTH', '{SHOULDERS,TRICEPS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/67n3r98.gif', 'https://static.exercisedb.dev/media/67n3r98.gif', 'Sit in the shoulder press machine with back supported, grip handles at shoulder height and press overhead to full extension, lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Shoulder Press Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Assisted Pull Up Machine', 'STRENGTH', '{BACK,BICEPS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/kiJ4Z2K.gif', 'https://static.exercisedb.dev/media/kiJ4Z2K.gif', 'Kneel or stand on the assisted pull-up platform, grip the bar overhead, pull chest to the bar while the counterweight reduces effective load, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Assisted Pull Up Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Seated Row Machine', 'STRENGTH', '{BACK,BICEPS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/7I6LNUG.gif', 'https://static.exercisedb.dev/media/7I6LNUG.gif', 'Sit at the seated row machine with chest against the pad, grip the handles and row to your midsection driving elbows back, squeeze shoulder blades and return.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Seated Row Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Preacher Curl Machine', 'STRENGTH', '{BICEPS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/b6hQYMb.gif', 'https://static.exercisedb.dev/media/b6hQYMb.gif', 'Sit at the preacher curl machine with upper arms flat on the angled pad, curl the handles up to full contraction and lower fully for maximum stretch.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Preacher Curl Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Leg Curl Machine', 'STRENGTH', '{HAMSTRINGS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/17lJ1kr.gif', 'https://static.exercisedb.dev/media/17lJ1kr.gif', 'Lie or sit in the leg curl machine with pad positioned just above the heels, curl legs toward glutes against resistance, lower slowly to the starting position.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Leg Curl Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Leg Extension Machine', 'STRENGTH', '{QUADRICEPS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/my33uHU.gif', 'https://static.exercisedb.dev/media/my33uHU.gif', 'Sit in the leg extension machine with pad on the shins just above the ankle, extend legs to full lockout squeezing the quads, lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Leg Extension Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Hip Abductor Machine', 'STRENGTH', '{GLUTES,HIP_ABDUCTORS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/CHpahtl.gif', 'https://static.exercisedb.dev/media/CHpahtl.gif', 'Sit in the hip abductor machine with pads on the outer thighs, push legs outward against resistance as far as possible, return slowly with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Hip Abductor Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Hip Adductor Machine', 'STRENGTH', '{ADDUCTORS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/oHsrypV.gif', 'https://static.exercisedb.dev/media/oHsrypV.gif', 'Sit in the hip adductor machine with pads on the inner thighs, squeeze legs together against resistance, return slowly to the starting position.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Hip Adductor Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Calf Raise Machine', 'STRENGTH', '{CALVES}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/ykUOVze.gif', 'https://static.exercisedb.dev/media/ykUOVze.gif', 'Stand in the calf raise machine with shoulders under the pads and balls of feet on the platform, raise onto toes as high as possible, lower heels below platform level for a full stretch.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Calf Raise Machine' AND is_system = TRUE);

-- ----------------------------------------
-- V022 NEW EXERCISES: RESISTANCE BANDS (~8)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Pull Apart', 'STRENGTH', '{BACK,SHOULDERS}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Hold a resistance band with both hands at shoulder height, pull it apart by extending arms wide until band touches your chest, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Pull Apart' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Face Pull', 'STRENGTH', '{SHOULDERS,BACK}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Anchor a band at face height, pull it toward your face with elbows flared high and externally rotate at the end position, return under control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Face Pull' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Squat', 'STRENGTH', '{LEGS,GLUTES}', '{RESISTANCE_BAND}', 'BEGINNER', 'https://static.exercisedb.dev/media/TUZLh71.gif', 'https://static.exercisedb.dev/media/TUZLh71.gif', 'Stand on a resistance band with feet shoulder-width, hold the ends at shoulder level, squat down to parallel keeping chest tall, drive back up to standing.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Good Morning', 'STRENGTH', '{HAMSTRINGS,GLUTES,BACK}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Stand on a band with feet hip-width and loop it over the back of your neck, hinge forward at the hips with slight knee bend, drive hips forward to return upright.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Good Morning' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Chest Press', 'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{RESISTANCE_BAND}', 'BEGINNER', 'https://static.exercisedb.dev/media/khlHMqs.gif', 'https://static.exercisedb.dev/media/khlHMqs.gif', 'Anchor a band behind you at chest height, grip the ends at chest level, press forward to full arm extension, return slowly against the band tension.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Chest Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Row', 'STRENGTH', '{BACK,BICEPS}', '{RESISTANCE_BAND}', 'BEGINNER', 'https://static.exercisedb.dev/media/km0sQC0.gif', 'https://static.exercisedb.dev/media/km0sQC0.gif', 'Anchor a band in front of you at waist height, grip the ends with both hands and row them toward your midsection driving elbows back, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Lateral Walk', 'STRENGTH', '{GLUTES,HIP_ABDUCTORS}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Place a band around your ankles or just above the knees, assume a quarter-squat position and step laterally keeping tension in the band throughout.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Lateral Walk' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Bicep Curl', 'STRENGTH', '{BICEPS}', '{RESISTANCE_BAND}', 'BEGINNER', 'https://static.exercisedb.dev/media/UNAB8ak.gif', 'https://static.exercisedb.dev/media/UNAB8ak.gif', 'Stand on a resistance band with feet hip-width, grip the ends with palms facing up, curl hands toward shoulders keeping elbows at sides, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Bicep Curl' AND is_system = TRUE);

-- ----------------------------------------
-- V022 NEW EXERCISES: CARDIO / OTHER (~5)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Battle Ropes', 'CARDIO', '{CARDIO,SHOULDERS,CORE,ARMS}', '{BATTLE_ROPES}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/RJa4tCo.gif', 'https://static.exercisedb.dev/media/RJa4tCo.gif', 'Hold one rope end in each hand, create alternating or simultaneous waves by moving arms up and down rapidly, maintain a stable athletic stance for the duration.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Battle Ropes' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Sled Push', 'CARDIO', '{CARDIO,LEGS,GLUTES,CORE}', '{SLED}', 'INTERMEDIATE', NULL, NULL, 'Load a sled to appropriate weight, grip the handles and drive it forward by pushing explosively through the legs, lean forward and take short powerful steps.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Sled Push' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Box Step Up', 'STRENGTH', '{LEGS,GLUTES}', '{BODYWEIGHT,PLYO_BOX}', 'BEGINNER', NULL, NULL, 'Stand facing a box or step, place one foot fully on the surface, press through that heel to step up bringing the trailing leg to meet it, step back down and alternate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Box Step Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Assault Bike', 'CARDIO', '{CARDIO,LEGS,ARMS,CORE}', '{MACHINE}', 'INTERMEDIATE', 'https://static.exercisedb.dev/media/H1PESYI.gif', 'https://static.exercisedb.dev/media/H1PESYI.gif', 'Sit on the assault bike, grip the moving handles and pedal as hard as possible using both arms and legs simultaneously, regulate intensity by adjusting speed.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Assault Bike' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Elliptical', 'CARDIO', '{CARDIO,LEGS,ARMS}', '{MACHINE}', 'BEGINNER', 'https://static.exercisedb.dev/media/rjtuP6X.gif', 'https://static.exercisedb.dev/media/rjtuP6X.gif', 'Step onto the elliptical and grip the handles, push and pull with arms while striding with legs in an elliptical motion, maintain an upright posture at the target heart rate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Elliptical' AND is_system = TRUE);

-- ============================================================
-- FOOD CATALOG (37 items from V019)
-- ============================================================

-- PROTEIN
INSERT INTO food_catalog (name, category, serving_size, calories, protein_grams, carbs_grams, fat_grams, fiber_grams, is_system, created_by_user_id)
SELECT name, category, serving_size, calories, protein_grams, carbs_grams, fat_grams, fiber_grams, is_system, created_by_user_id
FROM (VALUES
    ('Chicken Breast',     'PROTEIN', '100g',        165, 31.0, 0.0,  3.6,  0.0,  TRUE, NULL::TEXT),
    ('Salmon Fillet',      'PROTEIN', '100g',        208, 20.0, 0.0,  13.0, 0.0,  TRUE, NULL::TEXT),
    ('Ground Turkey',      'PROTEIN', '100g',        170, 21.0, 0.0,  9.0,  0.0,  TRUE, NULL::TEXT),
    ('Eggs',               'PROTEIN', '2 large',     143, 13.0, 1.0,  10.0, 0.0,  TRUE, NULL::TEXT),
    ('Greek Yogurt',       'PROTEIN', '170g',        100, 17.0, 6.0,  0.7,  0.0,  TRUE, NULL::TEXT),
    ('Whey Protein Shake', 'PROTEIN', '1 scoop',     120, 24.0, 3.0,  1.5,  0.0,  TRUE, NULL::TEXT),
    ('Tuna',               'PROTEIN', '100g',        130, 29.0, 0.0,  1.0,  0.0,  TRUE, NULL::TEXT),
    ('Steak Sirloin',      'PROTEIN', '100g',        206, 26.0, 0.0,  11.0, 0.0,  TRUE, NULL::TEXT),
    ('Tofu',               'PROTEIN', '100g',        76,  8.0,  2.0,  4.8,  0.3,  TRUE, NULL::TEXT),
    ('Shrimp',             'PROTEIN', '100g',        85,  20.0, 0.0,  0.5,  0.0,  TRUE, NULL::TEXT),
    -- CARB
    ('Brown Rice',         'CARB',    '1 cup cooked',216, 5.0,  45.0, 1.8,  3.5,  TRUE, NULL::TEXT),
    ('Sweet Potato',       'CARB',    '1 medium',    103, 2.0,  24.0, 0.1,  3.8,  TRUE, NULL::TEXT),
    ('Oatmeal',            'CARB',    '1 cup cooked',154, 5.0,  27.0, 2.6,  4.0,  TRUE, NULL::TEXT),
    ('Quinoa',             'CARB',    '1 cup cooked',222, 8.0,  39.0, 3.5,  5.0,  TRUE, NULL::TEXT),
    ('Whole Wheat Bread',  'CARB',    '2 slices',    160, 8.0,  28.0, 2.0,  4.0,  TRUE, NULL::TEXT),
    ('White Rice',         'CARB',    '1 cup cooked',206, 4.0,  45.0, 0.4,  0.6,  TRUE, NULL::TEXT),
    ('Pasta',              'CARB',    '1 cup cooked',220, 8.0,  43.0, 1.3,  2.5,  TRUE, NULL::TEXT),
    ('Banana',             'CARB',    '1 medium',    105, 1.3,  27.0, 0.4,  3.1,  TRUE, NULL::TEXT),
    -- FAT
    ('Avocado',            'FAT',     '1/2 medium',  160, 2.0,  9.0,  15.0, 6.7,  TRUE, NULL::TEXT),
    ('Almonds',            'FAT',     '28g',         164, 6.0,  6.0,  14.0, 3.5,  TRUE, NULL::TEXT),
    ('Peanut Butter',      'FAT',     '2 tbsp',      190, 7.0,  7.0,  16.0, 1.5,  TRUE, NULL::TEXT),
    ('Olive Oil',          'FAT',     '1 tbsp',      119, 0.0,  0.0,  13.5, 0.0,  TRUE, NULL::TEXT),
    ('Walnuts',            'FAT',     '28g',         185, 4.0,  4.0,  18.0, 1.9,  TRUE, NULL::TEXT),
    ('Coconut Oil',        'FAT',     '1 tbsp',      121, 0.0,  0.0,  13.5, 0.0,  TRUE, NULL::TEXT),
    -- VEGETABLE
    ('Broccoli',           'VEGETABLE','1 cup',      55,  4.0,  11.0, 0.6,  5.1,  TRUE, NULL::TEXT),
    ('Spinach',            'VEGETABLE','1 cup raw',  7,   1.0,  1.0,  0.1,  0.7,  TRUE, NULL::TEXT),
    ('Asparagus',          'VEGETABLE','1 cup',      27,  3.0,  5.0,  0.2,  2.8,  TRUE, NULL::TEXT),
    ('Bell Pepper',        'VEGETABLE','1 medium',   30,  1.0,  7.0,  0.3,  2.5,  TRUE, NULL::TEXT),
    ('Green Beans',        'VEGETABLE','1 cup',      31,  2.0,  7.0,  0.1,  4.0,  TRUE, NULL::TEXT),
    ('Kale',               'VEGETABLE','1 cup raw',  33,  3.0,  6.0,  0.6,  1.3,  TRUE, NULL::TEXT),
    -- FRUIT
    ('Apple',              'FRUIT',   '1 medium',    95,  0.5,  25.0, 0.3,  4.4,  TRUE, NULL::TEXT),
    ('Blueberries',        'FRUIT',   '1 cup',       85,  1.0,  21.0, 0.5,  3.6,  TRUE, NULL::TEXT),
    ('Strawberries',       'FRUIT',   '1 cup',       49,  1.0,  12.0, 0.5,  3.0,  TRUE, NULL::TEXT),
    ('Orange',             'FRUIT',   '1 medium',    62,  1.2,  15.0, 0.2,  3.1,  TRUE, NULL::TEXT),
    -- DAIRY
    ('Cottage Cheese',     'DAIRY',   '1 cup',       206, 28.0, 6.0,  9.0,  0.0,  TRUE, NULL::TEXT),
    ('Whole Milk',         'DAIRY',   '1 cup',       149, 8.0,  12.0, 8.0,  0.0,  TRUE, NULL::TEXT),
    ('Cheddar Cheese',     'DAIRY',   '28g',         113, 7.0,  0.4,  9.0,  0.0,  TRUE, NULL::TEXT),
    -- SNACK
    ('Protein Bar',        'SNACK',   '1 bar',       210, 20.0, 25.0, 7.0,  3.0,  TRUE, NULL::TEXT),
    ('Rice Cakes',         'SNACK',   '2 cakes',     70,  1.4,  15.0, 0.4,  0.4,  TRUE, NULL::TEXT),
    ('Trail Mix',          'SNACK',   '28g',         140, 4.0,  13.0, 9.0,  1.5,  TRUE, NULL::TEXT),
    ('Dark Chocolate',     'SNACK',   '28g',         155, 2.0,  17.0, 9.0,  2.0,  TRUE, NULL::TEXT)
) AS t(name, category, serving_size, calories, protein_grams, carbs_grams, fat_grams, fiber_grams, is_system, created_by_user_id)
WHERE NOT EXISTS (
    SELECT 1 FROM food_catalog fc WHERE fc.name = t.name
);
