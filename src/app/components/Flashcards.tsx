import { useState } from 'react';
import { Link } from 'react-router';
import { motion, AnimatePresence } from 'motion/react';
import { ArrowLeft, Volume2, ChevronRight, Star, Check } from 'lucide-react';

const flashcards = [
  { id: 1, word: 'Serendipity', definition: 'Finding something good without looking for it', example: 'Meeting my best friend was pure serendipity.' },
  { id: 2, word: 'Ephemeral', definition: 'Lasting for a very short time', example: 'The beauty of cherry blossoms is ephemeral.' },
  { id: 3, word: 'Resilient', definition: 'Able to recover quickly from difficulties', example: 'She remained resilient despite the challenges.' },
  { id: 4, word: 'Eloquent', definition: 'Fluent and persuasive in speaking or writing', example: 'His eloquent speech moved the audience.' },
  { id: 5, word: 'Ambiguous', definition: 'Open to more than one interpretation', example: 'The ending of the movie was ambiguous.' },
];

export function Flashcards() {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [learned, setLearned] = useState<number[]>([]);

  const currentCard = flashcards[currentIndex];
  const progress = ((currentIndex + 1) / flashcards.length) * 100;

  const handleNext = () => {
    setIsFlipped(false);
    if (currentIndex < flashcards.length - 1) {
      setCurrentIndex(currentIndex + 1);
    }
  };

  const handleMarkLearned = () => {
    if (!learned.includes(currentCard.id)) {
      setLearned([...learned, currentCard.id]);
    }
    handleNext();
  };

  return (
    <div className="min-h-screen bg-background pb-24">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-gradient-to-br from-primary to-accent p-6 rounded-b-[3rem] shadow-xl"
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
            Vocabulary Cards
          </h1>
          <div className="w-12 h-12 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center border border-white/30">
            <span className="text-white">{learned.length}/{flashcards.length}</span>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="bg-white/20 rounded-full h-3 overflow-hidden">
          <motion.div
            initial={{ width: 0 }}
            animate={{ width: `${progress}%` }}
            transition={{ duration: 0.5 }}
            className="h-full bg-white rounded-full"
          />
        </div>
      </motion.div>

      {/* Flashcard */}
      <div className="px-6 mt-12">
        <AnimatePresence mode="wait">
          <motion.div
            key={currentIndex}
            initial={{ opacity: 0, x: 100 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -100 }}
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
            className="mb-8"
          >
            <motion.div
              onClick={() => setIsFlipped(!isFlipped)}
              className="relative h-[400px] cursor-pointer"
              style={{ perspective: 1000 }}
            >
              <motion.div
                animate={{ rotateY: isFlipped ? 180 : 0 }}
                transition={{ duration: 0.6, type: "spring" }}
                style={{ transformStyle: "preserve-3d" }}
                className="w-full h-full"
              >
                {/* Front of Card */}
                <div
                  className="absolute inset-0 bg-gradient-to-br from-card to-card shadow-2xl rounded-[3rem] p-8 flex flex-col items-center justify-center border-2 border-primary/20"
                  style={{ backfaceVisibility: "hidden" }}
                >
                  <div className="text-center">
                    <motion.div
                      whileHover={{ scale: 1.1 }}
                      className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6"
                    >
                      <Volume2 className="w-8 h-8 text-primary" />
                    </motion.div>
                    <h2 style={{ fontFamily: 'var(--font-display)' }} className="text-5xl mb-4 text-foreground">
                      {currentCard.word}
                    </h2>
                    <p className="text-muted-foreground">Tap to see definition</p>
                  </div>
                  <div className="absolute bottom-8 left-8 right-8">
                    <div className="flex items-center justify-center gap-2">
                      {[...Array(3)].map((_, i) => (
                        <div
                          key={i}
                          className={`w-2 h-2 rounded-full ${
                            i === 0 ? 'bg-primary' : 'bg-muted'
                          }`}
                        />
                      ))}
                    </div>
                  </div>
                </div>

                {/* Back of Card */}
                <div
                  className="absolute inset-0 bg-gradient-to-br from-accent to-accent/90 shadow-2xl rounded-[3rem] p-8 flex flex-col justify-center"
                  style={{ backfaceVisibility: "hidden", transform: "rotateY(180deg)" }}
                >
                  <div className="mb-6">
                    <div className="inline-block px-4 py-2 bg-white/20 rounded-2xl mb-4">
                      <p className="text-white/80 text-sm">Definition</p>
                    </div>
                    <p className="text-white text-xl mb-6">
                      {currentCard.definition}
                    </p>
                  </div>
                  <div>
                    <div className="inline-block px-4 py-2 bg-white/20 rounded-2xl mb-4">
                      <p className="text-white/80 text-sm">Example</p>
                    </div>
                    <p className="text-white/90 italic">
                      "{currentCard.example}"
                    </p>
                  </div>
                </div>
              </motion.div>
            </motion.div>
          </motion.div>
        </AnimatePresence>

        {/* Action Buttons */}
        <div className="space-y-3">
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={handleMarkLearned}
            className="w-full py-5 bg-gradient-to-r from-primary to-primary/90 text-white rounded-3xl shadow-lg shadow-primary/30 flex items-center justify-center gap-2"
          >
            <Check className="w-6 h-6" />
            <span style={{ fontFamily: 'var(--font-display)' }} className="text-lg">
              I know this word
            </span>
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={handleNext}
            disabled={currentIndex === flashcards.length - 1}
            className="w-full py-5 bg-card border-2 border-border text-foreground rounded-3xl shadow-lg flex items-center justify-center gap-2 disabled:opacity-50"
          >
            <span style={{ fontFamily: 'var(--font-display)' }} className="text-lg">
              Skip
            </span>
            <ChevronRight className="w-6 h-6" />
          </motion.button>
        </div>

        {/* Stats */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="mt-8 grid grid-cols-3 gap-4"
        >
          <div className="bg-card rounded-2xl p-4 text-center border border-border">
            <Star className="w-6 h-6 text-primary mx-auto mb-2" />
            <p className="text-2xl" style={{ fontFamily: 'var(--font-display)' }}>
              {learned.length}
            </p>
            <p className="text-xs text-muted-foreground">Learned</p>
          </div>
          <div className="bg-card rounded-2xl p-4 text-center border border-border">
            <Volume2 className="w-6 h-6 text-secondary mx-auto mb-2" />
            <p className="text-2xl" style={{ fontFamily: 'var(--font-display)' }}>
              {flashcards.length}
            </p>
            <p className="text-xs text-muted-foreground">Total</p>
          </div>
          <div className="bg-card rounded-2xl p-4 text-center border border-border">
            <Check className="w-6 h-6 text-accent mx-auto mb-2" />
            <p className="text-2xl" style={{ fontFamily: 'var(--font-display)' }}>
              {Math.round(progress)}%
            </p>
            <p className="text-xs text-muted-foreground">Progress</p>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
