import { Link } from 'react-router';
import { motion } from 'motion/react';
import { ArrowLeft, User, Trophy, Flame, BookOpen, Target, Award, Star, TrendingUp } from 'lucide-react';
import * as Progress from '@radix-ui/react-progress';

const achievements = [
  { id: 1, name: '7 Day Streak', icon: Flame, color: 'from-secondary to-secondary/80', unlocked: true },
  { id: 2, name: 'First Quiz', icon: Target, color: 'from-accent to-accent/80', unlocked: true },
  { id: 3, name: '100 Words', icon: BookOpen, color: 'from-primary to-primary/80', unlocked: true },
  { id: 4, name: 'Perfect Score', icon: Star, color: 'from-secondary to-accent', unlocked: false },
  { id: 5, name: '30 Day Streak', icon: Award, color: 'from-primary to-accent', unlocked: false },
  { id: 6, name: 'Speed Learner', icon: TrendingUp, color: 'from-accent to-primary', unlocked: false },
];

const weeklyProgress = [
  { day: 'Mon', value: 80 },
  { day: 'Tue', value: 90 },
  { day: 'Wed', value: 75 },
  { day: 'Thu', value: 95 },
  { day: 'Fri', value: 100 },
  { day: 'Sat', value: 70 },
  { day: 'Sun', value: 85 },
];

export function Profile() {
  return (
    <div className="min-h-screen bg-background pb-24">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-gradient-to-br from-accent via-accent to-primary p-6 pb-16 rounded-b-[3rem] shadow-xl"
      >
        <div className="flex items-center justify-between mb-8">
          <Link to="/dashboard">
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="w-12 h-12 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center border border-white/30"
            >
              <ArrowLeft className="w-6 h-6 text-white" />
            </motion.button>
          </Link>
          <h1 style={{ fontFamily: 'var(--font-display)' }} className="text-2xl text-white">
            Profile
          </h1>
          <div className="w-12" />
        </div>

        {/* Profile Card */}
        <motion.div
          initial={{ scale: 0.95, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="text-center"
        >
          <div className="w-24 h-24 bg-white rounded-full mx-auto mb-4 flex items-center justify-center shadow-xl border-4 border-white/30">
            <User className="w-12 h-12 text-accent" />
          </div>
          <h2 style={{ fontFamily: 'var(--font-display)' }} className="text-3xl text-white mb-2">
            Alex Johnson
          </h2>
          <p className="text-white/80">Learning since March 2026</p>
        </motion.div>
      </motion.div>

      {/* Stats Grid */}
      <div className="px-6 -mt-8 mb-6">
        <div className="grid grid-cols-3 gap-3">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="bg-card rounded-2xl p-4 text-center shadow-lg border border-border"
          >
            <Trophy className="w-6 h-6 text-primary mx-auto mb-2" />
            <p className="text-2xl mb-1" style={{ fontFamily: 'var(--font-display)' }}>
              1250
            </p>
            <p className="text-xs text-muted-foreground">Points</p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
            className="bg-card rounded-2xl p-4 text-center shadow-lg border border-border"
          >
            <Flame className="w-6 h-6 text-secondary mx-auto mb-2" />
            <p className="text-2xl mb-1" style={{ fontFamily: 'var(--font-display)' }}>
              7
            </p>
            <p className="text-xs text-muted-foreground">Day Streak</p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            className="bg-card rounded-2xl p-4 text-center shadow-lg border border-border"
          >
            <BookOpen className="w-6 h-6 text-accent mx-auto mb-2" />
            <p className="text-2xl mb-1" style={{ fontFamily: 'var(--font-display)' }}>
              142
            </p>
            <p className="text-xs text-muted-foreground">Words</p>
          </motion.div>
        </div>
      </div>

      {/* Weekly Activity */}
      <div className="px-6 mb-6">
        <h3 style={{ fontFamily: 'var(--font-display)' }} className="text-xl mb-4">
          Weekly Activity
        </h3>
        <div className="bg-card rounded-3xl p-6 shadow-lg border border-border">
          <div className="flex items-end justify-between h-32 gap-2">
            {weeklyProgress.map((day, index) => (
              <motion.div
                key={day.day}
                initial={{ height: 0 }}
                animate={{ height: `${day.value}%` }}
                transition={{ delay: 0.6 + index * 0.1, duration: 0.5 }}
                className="flex-1 flex flex-col items-center"
              >
                <div className="w-full bg-gradient-to-t from-primary to-accent rounded-xl relative overflow-hidden">
                  <div className="absolute inset-0 bg-white/20" />
                </div>
                <p className="text-xs text-muted-foreground mt-2">{day.day}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </div>

      {/* Learning Progress */}
      <div className="px-6 mb-6">
        <h3 style={{ fontFamily: 'var(--font-display)' }} className="text-xl mb-4">
          Learning Progress
        </h3>
        <div className="space-y-4">
          <div className="bg-card rounded-2xl p-5 shadow-lg border border-border">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center">
                  <BookOpen className="w-5 h-5 text-primary" />
                </div>
                <div>
                  <p className="font-medium">Vocabulary</p>
                  <p className="text-xs text-muted-foreground">142 / 200 words</p>
                </div>
              </div>
              <span className="text-primary">71%</span>
            </div>
            <Progress.Root className="h-2 bg-muted rounded-full overflow-hidden">
              <Progress.Indicator
                className="h-full bg-gradient-to-r from-primary to-primary/80 rounded-full transition-transform duration-500"
                style={{ transform: `translateX(-${100 - 71}%)` }}
              />
            </Progress.Root>
          </div>

          <div className="bg-card rounded-2xl p-5 shadow-lg border border-border">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-secondary/10 rounded-xl flex items-center justify-center">
                  <Target className="w-5 h-5 text-secondary" />
                </div>
                <div>
                  <p className="font-medium">Quizzes</p>
                  <p className="text-xs text-muted-foreground">18 / 25 completed</p>
                </div>
              </div>
              <span className="text-secondary">72%</span>
            </div>
            <Progress.Root className="h-2 bg-muted rounded-full overflow-hidden">
              <Progress.Indicator
                className="h-full bg-gradient-to-r from-secondary to-secondary/80 rounded-full transition-transform duration-500"
                style={{ transform: `translateX(-${100 - 72}%)` }}
              />
            </Progress.Root>
          </div>
        </div>
      </div>

      {/* Achievements */}
      <div className="px-6 mb-6">
        <h3 style={{ fontFamily: 'var(--font-display)' }} className="text-xl mb-4">
          Achievements
        </h3>
        <div className="grid grid-cols-3 gap-4">
          {achievements.map((achievement, index) => {
            const Icon = achievement.icon;
            return (
              <motion.div
                key={achievement.id}
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.8 + index * 0.1 }}
                className={`rounded-2xl p-4 text-center shadow-lg ${
                  achievement.unlocked
                    ? `bg-gradient-to-br ${achievement.color}`
                    : 'bg-muted/50 opacity-50'
                }`}
              >
                <div
                  className={`w-12 h-12 rounded-xl mx-auto mb-2 flex items-center justify-center ${
                    achievement.unlocked
                      ? 'bg-white/20 backdrop-blur-sm'
                      : 'bg-white/10'
                  }`}
                >
                  <Icon
                    className={`w-6 h-6 ${
                      achievement.unlocked ? 'text-white' : 'text-muted-foreground'
                    }`}
                  />
                </div>
                <p
                  className={`text-xs ${
                    achievement.unlocked ? 'text-white' : 'text-muted-foreground'
                  }`}
                >
                  {achievement.name}
                </p>
              </motion.div>
            );
          })}
        </div>
      </div>

      {/* Settings */}
      <div className="px-6">
        <h3 style={{ fontFamily: 'var(--font-display)' }} className="text-xl mb-4">
          Settings
        </h3>
        <div className="space-y-3">
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="w-full bg-card border border-border rounded-2xl p-5 text-left shadow-lg"
          >
            <p className="font-medium">Edit Profile</p>
            <p className="text-sm text-muted-foreground">Update your information</p>
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="w-full bg-card border border-border rounded-2xl p-5 text-left shadow-lg"
          >
            <p className="font-medium">Notifications</p>
            <p className="text-sm text-muted-foreground">Manage your reminders</p>
          </motion.button>

          <Link to="/">
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="w-full bg-card border border-destructive/30 rounded-2xl p-5 text-left shadow-lg"
            >
              <p className="font-medium text-destructive">Sign Out</p>
              <p className="text-sm text-muted-foreground">See you soon!</p>
            </motion.button>
          </Link>
        </div>
      </div>
    </div>
  );
}
