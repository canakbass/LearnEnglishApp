import { useState } from 'react';
import { Link } from 'react-router';
import { motion, AnimatePresence } from 'motion/react';
import { ArrowLeft, Trophy, CheckCircle2, XCircle, Sparkles } from 'lucide-react';
import confetti from 'canvas-confetti';

const questions = [
  {
    id: 1,
    question: 'What does "ubiquitous" mean?',
    options: [
      'Very rare',
      'Present everywhere',
      'Extremely large',
      'Difficult to find'
    ],
    correct: 1
  },
  {
    id: 2,
    question: 'Choose the correct sentence:',
    options: [
      'She don\'t like apples',
      'She doesn\'t likes apples',
      'She doesn\'t like apples',
      'She not like apples'
    ],
    correct: 2
  },
  {
    id: 3,
    question: 'What is a synonym for "happy"?',
    options: [
      'Sad',
      'Angry',
      'Joyful',
      'Tired'
    ],
    correct: 2
  },
  {
    id: 4,
    question: 'Which word is an adjective?',
    options: [
      'Run',
      'Quickly',
      'Beautiful',
      'Happiness'
    ],
    correct: 2
  },
  {
    id: 5,
    question: 'What does "benevolent" mean?',
    options: [
      'Evil and cruel',
      'Kind and generous',
      'Angry and hostile',
      'Lazy and idle'
    ],
    correct: 1
  }
];

export function Quiz() {
  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
  const [isAnswered, setIsAnswered] = useState(false);
  const [score, setScore] = useState(0);
  const [showResults, setShowResults] = useState(false);

  const question = questions[currentQuestion];
  const progress = ((currentQuestion + 1) / questions.length) * 100;

  const handleSelectAnswer = (index: number) => {
    if (isAnswered) return;

    setSelectedAnswer(index);
    setIsAnswered(true);

    if (index === question.correct) {
      setScore(score + 1);
      confetti({
        particleCount: 50,
        spread: 60,
        origin: { y: 0.6 }
      });
    }
  };

  const handleNext = () => {
    if (currentQuestion < questions.length - 1) {
      setCurrentQuestion(currentQuestion + 1);
      setSelectedAnswer(null);
      setIsAnswered(false);
    } else {
      setShowResults(true);
      if (score >= 4) {
        confetti({
          particleCount: 100,
          spread: 70,
          origin: { y: 0.6 }
        });
      }
    }
  };

  const handleRestart = () => {
    setCurrentQuestion(0);
    setSelectedAnswer(null);
    setIsAnswered(false);
    setScore(0);
    setShowResults(false);
  };

  if (showResults) {
    const percentage = (score / questions.length) * 100;
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          className="w-full max-w-md"
        >
          <div className="bg-gradient-to-br from-primary to-accent rounded-[3rem] p-8 shadow-2xl text-center">
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
              className="w-24 h-24 bg-white/20 rounded-full flex items-center justify-center mx-auto mb-6"
            >
              <Trophy className="w-12 h-12 text-white" />
            </motion.div>

            <h2 style={{ fontFamily: 'var(--font-display)' }} className="text-4xl text-white mb-4">
              Quiz Complete!
            </h2>

            <p className="text-white/90 mb-6">
              You scored {score} out of {questions.length}
            </p>

            <div className="bg-white/20 rounded-3xl p-6 mb-6">
              <div className="text-6xl mb-2" style={{ fontFamily: 'var(--font-display)' }}>
                <span className="text-white">{percentage}%</span>
              </div>
              <p className="text-white/80">
                {percentage >= 80 ? 'Excellent!' : percentage >= 60 ? 'Good job!' : 'Keep practicing!'}
              </p>
            </div>

            <div className="space-y-3">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleRestart}
                className="w-full py-4 bg-white text-primary rounded-2xl shadow-lg"
              >
                <span style={{ fontFamily: 'var(--font-display)' }}>Try Again</span>
              </motion.button>

              <Link to="/dashboard">
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  className="w-full py-4 bg-white/20 text-white rounded-2xl border border-white/30"
                >
                  <span style={{ fontFamily: 'var(--font-display)' }}>Back to Dashboard</span>
                </motion.button>
              </Link>
            </div>
          </div>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-24">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-gradient-to-br from-secondary to-accent p-6 rounded-b-[3rem] shadow-xl"
      >
        <div className="flex items-center justify-between mb-6">
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
            Practice Quiz
          </h1>
          <div className="w-12 h-12 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center border border-white/30">
            <span className="text-white">{currentQuestion + 1}/{questions.length}</span>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="bg-white/20 rounded-full h-3 overflow-hidden">
          <motion.div
            animate={{ width: `${progress}%` }}
            transition={{ duration: 0.5 }}
            className="h-full bg-white rounded-full"
          />
        </div>
      </motion.div>

      {/* Question */}
      <div className="px-6 mt-8">
        <AnimatePresence mode="wait">
          <motion.div
            key={currentQuestion}
            initial={{ opacity: 0, x: 100 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -100 }}
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
          >
            {/* Question Card */}
            <div className="bg-gradient-to-br from-card to-card shadow-xl rounded-3xl p-8 mb-6 border-2 border-secondary/20">
              <div className="flex items-start gap-3 mb-4">
                <div className="w-10 h-10 bg-secondary/10 rounded-2xl flex items-center justify-center flex-shrink-0">
                  <Sparkles className="w-5 h-5 text-secondary" />
                </div>
                <h2 className="text-xl leading-relaxed">
                  {question.question}
                </h2>
              </div>
            </div>

            {/* Options */}
            <div className="space-y-3 mb-6">
              {question.options.map((option, index) => {
                const isSelected = selectedAnswer === index;
                const isCorrect = index === question.correct;
                const showCorrect = isAnswered && isCorrect;
                const showIncorrect = isAnswered && isSelected && !isCorrect;

                return (
                  <motion.button
                    key={index}
                    whileHover={{ scale: isAnswered ? 1 : 1.02 }}
                    whileTap={{ scale: isAnswered ? 1 : 0.98 }}
                    onClick={() => handleSelectAnswer(index)}
                    disabled={isAnswered}
                    className={`w-full p-5 rounded-2xl border-2 transition-all text-left flex items-center justify-between ${
                      showCorrect
                        ? 'bg-primary/10 border-primary shadow-lg shadow-primary/20'
                        : showIncorrect
                        ? 'bg-destructive/10 border-destructive shadow-lg shadow-destructive/20'
                        : isSelected
                        ? 'bg-secondary/10 border-secondary'
                        : 'bg-card border-border hover:border-secondary/50'
                    }`}
                  >
                    <div className="flex items-center gap-4">
                      <div
                        className={`w-10 h-10 rounded-full flex items-center justify-center transition-all ${
                          showCorrect
                            ? 'bg-primary text-white'
                            : showIncorrect
                            ? 'bg-destructive text-white'
                            : 'bg-muted text-foreground'
                        }`}
                      >
                        {String.fromCharCode(65 + index)}
                      </div>
                      <span className="text-base">{option}</span>
                    </div>
                    {showCorrect && <CheckCircle2 className="w-6 h-6 text-primary flex-shrink-0" />}
                    {showIncorrect && <XCircle className="w-6 h-6 text-destructive flex-shrink-0" />}
                  </motion.button>
                );
              })}
            </div>

            {/* Next Button */}
            {isAnswered && (
              <motion.button
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleNext}
                className="w-full py-5 bg-gradient-to-r from-secondary to-secondary/90 text-white rounded-3xl shadow-lg shadow-secondary/30"
              >
                <span style={{ fontFamily: 'var(--font-display)' }} className="text-lg">
                  {currentQuestion < questions.length - 1 ? 'Next Question' : 'See Results'}
                </span>
              </motion.button>
            )}
          </motion.div>
        </AnimatePresence>

        {/* Score Display */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="mt-8 bg-card rounded-3xl p-6 border border-border text-center"
        >
          <p className="text-muted-foreground mb-2">Current Score</p>
          <p className="text-4xl" style={{ fontFamily: 'var(--font-display)' }}>
            {score} / {currentQuestion + (isAnswered ? 1 : 0)}
          </p>
        </motion.div>
      </div>
    </div>
  );
}
