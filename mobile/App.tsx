import React, { useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import * as FileSystem from 'expo-file-system';

type Step =
  | 'login'
  | 'profile'
  | 'workoutQuestions'
  | 'workoutResult'
  | 'dietQuestions'
  | 'dietResult';

type WorkoutResponse = unknown;
type DietResponse = unknown;

const API_BASE_URL = 'http://localhost:8080'; // Change to deployed backend URL when needed

async function saveToObjectStorageFolder(kind: 'workout' | 'diet', payload: unknown) {
  const base = `${FileSystem.documentDirectory}object-storage/`;
  const file = `${base}${kind}-${new Date().toISOString().replace(/[:.]/g, '-')}.json`;

  const info = await FileSystem.getInfoAsync(base);
  if (!info.exists) {
    await FileSystem.makeDirectoryAsync(base, { intermediates: true });
  }

  await FileSystem.writeAsStringAsync(file, JSON.stringify(payload, null, 2));
  return file;
}

export default function App() {
  const [step, setStep] = useState<Step>('login');
  const [loading, setLoading] = useState(false);

  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [token, setToken] = useState('');

  const [gender, setGender] = useState('');
  const [weight, setWeight] = useState('');
  const [age, setAge] = useState('');

  const [equipment, setEquipment] = useState('');
  const [minutesPerDay, setMinutesPerDay] = useState('45');

  const [physiqueGoal, setPhysiqueGoal] = useState('muscle gain');
  const [dietaryLimits, setDietaryLimits] = useState('none');
  const [preferredProteins, setPreferredProteins] = useState('chicken, fish');

  const [workoutResult, setWorkoutResult] = useState<WorkoutResponse | null>(null);
  const [dietResult, setDietResult] = useState<DietResponse | null>(null);

  const canLogin = useMemo(() => usernameOrEmail.trim() && password.trim(), [usernameOrEmail, password]);

  const login = async () => {
    if (!canLogin) return;

    try {
      setLoading(true);
      const res = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ usernameOrEmail, password }),
      });

      if (!res.ok) {
        throw new Error(`Login failed (${res.status})`);
      }

      const json = await res.json();
      const jwt = json?.token || json?.accessToken || '';
      if (!jwt) throw new Error('No token returned from backend');

      setToken(jwt);
      setStep('profile');
    } catch (e: any) {
      Alert.alert('Login error', e?.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  const submitWorkout = async () => {
    try {
      setLoading(true);

      const weeklyFrequency = Math.max(1, Math.round((Number(minutesPerDay) || 0) / 20));

      const res = await fetch(`${API_BASE_URL}/api/v1/workout-plans/generate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          age: Number(age),
          equipment,
          weekly_frequency: weeklyFrequency,
          gender,
          weight: Number(weight),
          minutes_per_day: Number(minutesPerDay),
        }),
      });

      if (!res.ok) throw new Error(`Workout generation failed (${res.status})`);

      const json = await res.json();
      setWorkoutResult(json);
      const path = await saveToObjectStorageFolder('workout', json);
      Alert.alert('Workout profile saved', `Stored at: ${path}`);
      setStep('workoutResult');
    } catch (e: any) {
      Alert.alert('Workout error', e?.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  const submitDiet = async () => {
    try {
      setLoading(true);

      const proteins = preferredProteins
        .split(',')
        .map((p) => p.trim())
        .filter(Boolean);

      const limits = dietaryLimits.toLowerCase() === 'none' ? [] : dietaryLimits.split(',').map((d) => d.trim());

      const res = await fetch(`${API_BASE_URL}/api/v1/diet-plans/generate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          preferred_proteins: proteins,
          preferred_carbs: ['rice', 'potatoes'],
          preferred_fats: ['olive oil', 'avocado'],
          diet_goals: physiqueGoal,
          dietary_limitations: limits,
          meals_per_day: 3,
          target_calories: 2400,
        }),
      });

      if (!res.ok) throw new Error(`Diet generation failed (${res.status})`);

      const json = await res.json();
      setDietResult(json);
      const path = await saveToObjectStorageFolder('diet', json);
      Alert.alert('Diet profile saved', `Stored at: ${path}`);
      setStep('dietResult');
    } catch (e: any) {
      Alert.alert('Diet error', e?.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  const PrimaryButton = ({ text, onPress, disabled = false }: { text: string; onPress: () => void; disabled?: boolean }) => (
    <Pressable onPress={onPress} disabled={disabled} style={[styles.button, disabled && styles.buttonDisabled]}>
      <Text style={styles.buttonText}>{text}</Text>
    </Pressable>
  );

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.title}>BYB</Text>
        <Text style={styles.subtitle}>Build Your Body — iOS frontend MVP</Text>

        {step === 'login' && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Login</Text>
            <TextInput placeholder="Username or Email" style={styles.input} value={usernameOrEmail} onChangeText={setUsernameOrEmail} autoCapitalize="none" />
            <TextInput placeholder="Password" style={styles.input} value={password} onChangeText={setPassword} secureTextEntry />
            <PrimaryButton text="Continue" onPress={login} disabled={!canLogin || loading} />
          </View>
        )}

        {step === 'profile' && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Profile Basics</Text>
            <TextInput placeholder="Gender" style={styles.input} value={gender} onChangeText={setGender} />
            <TextInput placeholder="Weight (lbs)" keyboardType="numeric" style={styles.input} value={weight} onChangeText={setWeight} />
            <TextInput placeholder="Age" keyboardType="numeric" style={styles.input} value={age} onChangeText={setAge} />
            <PrimaryButton text="Next: Workout Questions" onPress={() => setStep('workoutQuestions')} disabled={!gender || !weight || !age} />
          </View>
        )}

        {step === 'workoutQuestions' && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Workout Questions</Text>
            <TextInput placeholder="Preferred equipment (e.g. dumbbells, machines)" style={styles.input} value={equipment} onChangeText={setEquipment} />
            <TextInput placeholder="Minutes per day" keyboardType="numeric" style={styles.input} value={minutesPerDay} onChangeText={setMinutesPerDay} />
            <PrimaryButton text="Generate Workout Profile" onPress={submitWorkout} disabled={!equipment || !minutesPerDay || loading} />
          </View>
        )}

        {step === 'workoutResult' && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Workout Profile Ready</Text>
            <Text style={styles.code}>{JSON.stringify(workoutResult, null, 2)}</Text>
            <PrimaryButton text="Next: Diet Questions" onPress={() => setStep('dietQuestions')} />
          </View>
        )}

        {step === 'dietQuestions' && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Diet Questions</Text>
            <TextInput placeholder="Current physique goal" style={styles.input} value={physiqueGoal} onChangeText={setPhysiqueGoal} />
            <TextInput placeholder="Dietary limitations (comma-separated)" style={styles.input} value={dietaryLimits} onChangeText={setDietaryLimits} />
            <TextInput placeholder="Preferred proteins (comma-separated)" style={styles.input} value={preferredProteins} onChangeText={setPreferredProteins} />
            <PrimaryButton text="Generate Diet Profile" onPress={submitDiet} disabled={!physiqueGoal || !preferredProteins || loading} />
          </View>
        )}

        {step === 'dietResult' && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Diet Profile Ready</Text>
            <Text style={styles.code}>{JSON.stringify(dietResult, null, 2)}</Text>
            <PrimaryButton text="Start Over" onPress={() => setStep('profile')} />
          </View>
        )}

        {loading && <ActivityIndicator style={{ marginTop: 12 }} />}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#0b1020' },
  container: { padding: 20, gap: 14 },
  title: { fontSize: 32, fontWeight: '700', color: 'white' },
  subtitle: { color: '#b6c2e0', marginBottom: 8 },
  card: {
    backgroundColor: '#121a30',
    borderRadius: 16,
    padding: 14,
    gap: 10,
  },
  cardTitle: { color: 'white', fontSize: 18, fontWeight: '600' },
  input: {
    backgroundColor: '#1b2746',
    color: 'white',
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  button: {
    backgroundColor: '#5d7cff',
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  buttonText: {
    color: 'white',
    fontWeight: '700',
  },
  code: {
    color: '#b6c2e0',
    backgroundColor: '#0e1427',
    borderRadius: 8,
    padding: 10,
    fontSize: 12,
  },
});
