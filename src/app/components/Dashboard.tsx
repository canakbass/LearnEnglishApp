import { Link } from 'react-router';
import { motion } from 'motion/react';
import { BookOpen, Trophy, Flame, Target, Zap, User } from 'lucide-react';
import * as Progress from '@radix-ui/react-progress';

export function Dashboard() {
  const dailyGoal = 75;
  const weekStreak = 7;
  const totalPoints = 1250;

  return (
    <div className="min-h-screen bg-background pb-24">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-gradient-to-br from-primary via-primary to-accent p-6 pb-12 rounded-b-[3rem] shadow-xl"
      >
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 style={{ fontFamily: 'var(--font-display)' }} className="text-3xl text-white mb-1">
              Hello, Alex! 👋
            </h1>
            <p className="text-white/80">Keep up the great work!</p>
          </div>
          <Link to="/profile">
            <motion.div
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="w-14 h-14 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center border-2 border-white/30"
            >
              <User className="w-7 h-7 text-white" />
            </motion.div>
          </Link>
        </div>

        {/* Daily Goal Progress */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.2 }}
          className="bg-white/20 backdrop-blur-md rounded-3xl p-5 border border-white/30"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Target className="w-5 h-5 text-white" />
              <span className="text-white">Daily Goal</span>
            </div>
            <span className="text-white">{dailyGoal}%</span>
          </div>
          <Progress.Root className="h-3 bg-white/20 rounded-full overflow-hidden">
            <Progress.Indicator
              className="h-full bg-gradient-to-r from-white to-white/90 rounded-full transition-transform duration-500"
              style={{ transform: `translateX(-${100 - dailyGoal}%)` }}
            />
          </Progress.Root>
        </motion.div>
      </motion.div>

      {/* Stats Cards */}
      <div className="px-6 -mt-8 mb-6">
        <div className="grid grid-cols-2 gap-4">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 }}
            className="bg-gradient-to-br from-secondary to-secondary/90 rounded-3xl p-5 shadow-lg shadow-secondary/20"
          >
            <div className="flex items-center gap-3 mb-2">
              <div className="w-12 h-12 bg-white/20 rounded-2xl flex items-center justify-center">
                <Flame className="w-6 h-6 text-white" />
              </div>
              <div>
                <p className="text-white/80 text-sm">Streak</p>
                <p style={{ fontFamily: 'var(--font-display)' }} className="text-2xl text-white">
                  {weekStreak} days
                </p>
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.4 }}
            className="bg-gradient-to-br from-accent to-accent/90 rounded-3xl p-5 shadow-lg shadow-accent/20"
          >
            <div className="flex items-center gap-3 mb-2">
              <div className="w-12 h-12 bg-white/20 rounded-2xl flex items-center justify-center">
                <Trophy className="w-6 h-6 text-white" />
              </div>
              <div>
                <p className="text-white/80 text-sm">Points</p>
                <p style={{ fontFamily: 'var(--font-display)' }} className="text-2xl text-white">
                  {totalPoints}
                </p>
              </div>
            </div>
          </motion.div>
        </div>
      </div>

      {/* Learning Activities */}
      <div className="px-6">
        <h2 style={{ fontFamily: 'var(--font-display)' }} className="text-2xl mb-4">
          Continue Learning
        </h2>

        <div className="space-y-4">
          {/* Vocabulary Card */}
          <Link to="/flashcards">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="bg-card rounded-3xl p-6 shadow-lg hover:shadow-xl transition-all border border-border"
            >
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 bg-gradient-to-br from-primary to-primary/80 rounded-2xl flex items-center justify-center shadow-lg shadow-primary/20 rotate-3">
                  <BookOpen className="w-8 h-8 text-white" />
                </div>
                <div className="flex-1">
                  <h3 style={{ fontFamily: 'var(--font-display)' }} className="text-xl mb-1">
                    Vocabulary
                  </h3>
                  <p className="text-muted-foreground text-sm">Learn new words with flashcards</p>
                  <div className="mt-2">
                    <Progress.Root className="h-2 bg-muted rounded-full overflow-hidden">
                      <Progress.Indicator
                        className="h-full bg-primary rounded-full transition-transform duration-500"
                        style={{ transform: `translateX(-${100 - 60}%)` }}
                      />
                    </Progress.Root>
                    <p className="text-xs text-muted-foreground mt-1">60% complete</p>
                  </div>
                </div>
                <Zap className="w-6 h-6 text-primary" />
              </div>
            </motion.div>
          </Link>

          {/* Quiz Card */}
          <Link to="/quiz">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.6 }}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="bg-card rounded-3xl p-6 shadow-lg hover:shadow-xl transition-all border border-border"
            >
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 bg-gradient-to-br from-secondary to-secondary/80 rounded-2xl flex items-center justify-center shadow-lg shadow-secondary/20 -rotate-3">
                  <Target className="w-8 h-8 text-white" />
                </div>
                <div className="flex-1">
                  <h3 style={{ fontFamily: 'var(--font-display)' }} className="text-xl mb-1">
                    Practice Quiz
                  </h3>
                  <p className="text-muted-foreground text-sm">Test your knowledge</p>
                  <div className="mt-2">
                    <Progress.Root className="h-2 bg-muted rounded-full overflow-hidden">
                      <Progress.Indicator
                        className="h-full bg-secondary rounded-full transition-transform duration-500"
                        style={{ transform: `translateX(-${100 - 40}%)` }}
                      />
                    </Progress.Root>
                    <p className="text-xs text-muted-foreground mt-1">40% complete</p>
                  </div>
                </div>
                <Zap className="w-6 h-6 text-secondary" />
              </div>
            </motion.div>
          </Link>
        </div>
      </div>

      {/* Bottom Navigation */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.7 }}
        className="fixed bottom-0 left-0 right-0 bg-card border-t border-border px-6 py-4 rounded-t-3xl shadow-2xl"
      >
        <div className="flex items-center justify-around max-w-md mx-auto">
          <Link to="/dashboard">
            <motion.button
              whileTap={{ scale: 0.9 }}
              className="flex flex-col items-center gap-1"
            >
              <div className="w-12 h-12 bg-primary rounded-2xl flex items-center justify-center">
                <BookOpen className="w-6 h-6 text-white" />
              </div>
              <span className="text-xs text-primary">Learn</span>
            </motion.button>
          </Link>

          <Link to="/quiz">
            <motion.button
              whileTap={{ scale: 0.9 }}
              className="flex flex-col items-center gap-1"
            >
              <div className="w-12 h-12 bg-muted rounded-2xl flex items-center justify-center">
                <Target className="w-6 h-6 text-muted-foreground" />
              </div>
              <span className="text-xs text-muted-foreground">Practice</span>
            </motion.button>
          </Link>

          <Link to="/profile">
            <motion.button
              whileTap={{ scale: 0.9 }}
              className="flex flex-col items-center gap-1"
            >
              <div className="w-12 h-12 bg-muted rounded-2xl flex items-center justify-center">
                <User className="w-6 h-6 text-muted-foreground" />
              </div>
              <span className="text-xs text-muted-foreground">Profile</span>
            </motion.button>
          </Link>
        </div>
      </motion.div>
    </div>
  );
}
