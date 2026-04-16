import { Link } from 'react-router';
import { motion } from 'motion/react';
import { Sparkles, Mail, Lock, User } from 'lucide-react';

export function Register() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4 relative overflow-hidden">
      {/* Decorative background elements */}
      <div className="absolute top-0 right-0 w-96 h-96 bg-secondary/10 rounded-full blur-3xl" />
      <div className="absolute bottom-0 left-0 w-96 h-96 bg-accent/10 rounded-full blur-3xl" />
      <div className="absolute top-1/2 left-1/4 w-96 h-96 bg-primary/5 rounded-full blur-3xl" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="w-full max-w-md relative z-10"
      >
        {/* Logo and Header */}
        <motion.div
          initial={{ scale: 0.9 }}
          animate={{ scale: 1 }}
          transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
          className="text-center mb-8"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-secondary to-accent rounded-3xl mb-4 shadow-lg shadow-secondary/20 -rotate-3">
            <Sparkles className="w-10 h-10 text-white" />
          </div>
          <h1 style={{ fontFamily: 'var(--font-display)' }} className="text-4xl mb-2 text-foreground">
            Join LinguaPlay
          </h1>
          <p className="text-muted-foreground">Start your learning journey today</p>
        </motion.div>

        {/* Register Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.6 }}
          className="bg-card rounded-3xl p-8 shadow-2xl shadow-black/5"
        >
          <h2 style={{ fontFamily: 'var(--font-display)' }} className="text-2xl mb-6 text-center">
            Create Account
          </h2>

          <form className="space-y-4">
            {/* Name Input */}
            <motion.div
              whileTap={{ scale: 0.995 }}
              className="relative"
            >
              <label className="block text-sm mb-2 text-foreground/70">Full Name</label>
              <div className="relative">
                <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <input
                  type="text"
                  placeholder="John Doe"
                  className="w-full pl-12 pr-4 py-4 bg-input-background rounded-2xl border-2 border-transparent focus:border-secondary focus:outline-none transition-all"
                />
              </div>
            </motion.div>

            {/* Email Input */}
            <motion.div
              whileTap={{ scale: 0.995 }}
              className="relative"
            >
              <label className="block text-sm mb-2 text-foreground/70">Email</label>
              <div className="relative">
                <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <input
                  type="email"
                  placeholder="your@email.com"
                  className="w-full pl-12 pr-4 py-4 bg-input-background rounded-2xl border-2 border-transparent focus:border-secondary focus:outline-none transition-all"
                />
              </div>
            </motion.div>

            {/* Password Input */}
            <motion.div
              whileTap={{ scale: 0.995 }}
              className="relative"
            >
              <label className="block text-sm mb-2 text-foreground/70">Password</label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <input
                  type="password"
                  placeholder="••••••••"
                  className="w-full pl-12 pr-4 py-4 bg-input-background rounded-2xl border-2 border-transparent focus:border-secondary focus:outline-none transition-all"
                />
              </div>
            </motion.div>

            {/* Terms & Conditions */}
            <div className="flex items-start gap-3 pt-2">
              <input
                type="checkbox"
                id="terms"
                className="mt-1 w-4 h-4 accent-secondary rounded"
              />
              <label htmlFor="terms" className="text-sm text-muted-foreground">
                I agree to the Terms of Service and Privacy Policy
              </label>
            </div>

            {/* Register Button */}
            <Link to="/dashboard">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                type="button"
                className="w-full py-4 bg-gradient-to-r from-secondary to-secondary/90 text-white rounded-2xl shadow-lg shadow-secondary/30 hover:shadow-xl hover:shadow-secondary/40 transition-all"
              >
                <span style={{ fontFamily: 'var(--font-display)' }}>Create Account</span>
              </motion.button>
            </Link>
          </form>

          {/* Login Link */}
          <div className="mt-6 text-center">
            <p className="text-muted-foreground">
              Already have an account?{' '}
              <Link to="/" className="text-secondary hover:underline">
                Log in
              </Link>
            </p>
          </div>
        </motion.div>

        {/* Footer */}
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.6 }}
          className="text-center text-sm text-muted-foreground mt-8"
        >
          Free forever. No credit card required. 💳
        </motion.p>
      </motion.div>
    </div>
  );
}
