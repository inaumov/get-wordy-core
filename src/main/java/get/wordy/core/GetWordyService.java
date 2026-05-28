package get.wordy.core;

import get.wordy.core.api.IUserCardsService;
import get.wordy.core.api.IVocabularyService;
import get.wordy.core.api.bean.*;
import get.wordy.core.api.bean.Vocabulary;
import get.wordy.core.api.bean.wrapper.VocabularySummary;
import get.wordy.core.api.exception.*;
import get.wordy.core.api.id.OwnerId;
import get.wordy.core.api.id.OwnersId;
import get.wordy.core.dao.exception.DaoException;
import get.wordy.core.dao.impl.ProgressDao;
import get.wordy.core.dao.impl.CardHeadlineDao;
import get.wordy.core.dao.impl.VocabularyDao;
import get.wordy.core.dao.impl.WordDao;
import get.wordy.core.db.LocalTxManager;
import get.wordy.core.api.bean.wrapper.Score;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GetWordyService implements IUserCardsService, IVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(GetWordyService.class);

    private VocabularyDao vocabularyDao;
    private WordDao wordDao;
    private ProgressDao progressDao;
    private CardHeadlineDao cardHeadlineDao;
    private LocalTxManager connection;
    private final Map<OwnerId, List<Vocabulary>> userVocabsCache = new HashMap<>();
    private final Map<String, List<Card>> cardsCache = new HashMap<>();
    private final Map<String, List<FlashCard>> exerciseCache = new HashMap<>();
    private final Map<Integer, List<Word>> wordsInVocabularyCache = new HashMap<>();

    @SuppressWarnings("unused")
    public GetWordyService() {
    }

    @SuppressWarnings("unused")
    public GetWordyService(VocabularyDao vocabularyDao,
                           WordDao wordDao,
                           ProgressDao progressDao,
                           CardHeadlineDao cardHeadlineDao,
                           LocalTxManager connection
    ) {
        this.vocabularyDao = vocabularyDao;
        this.wordDao = wordDao;
        this.progressDao = progressDao;
        this.cardHeadlineDao = cardHeadlineDao;
        this.connection = connection;
    }

    @Override
    public List<Vocabulary> getVocabularies(OwnerId ownerId) {

        List<Vocabulary> vocabularies = userVocabsCache.get(ownerId);
        if (vocabularies != null && !vocabularies.isEmpty()) {
            return vocabularies;
        }

        List<Vocabulary> list;
        try {
            // fetch from the database if not present in the cache
            list = vocabularyDao.selectAll(ownerId);

            // update the cache
            userVocabsCache.put(ownerId, list);
        } catch (DataAccessException e) {
            LOG.error("Error while loading vocabularies", e);
            return Collections.emptyList();
        }
        return list;
    }

    @Override
    public Vocabulary createVocabulary(OwnerId ownerId, String name, String pictureUrl) {
        Vocabulary vocabulary = new Vocabulary(name, pictureUrl);
        try {
            connection.open();
            Vocabulary saved = vocabularyDao.insert(ownerId, vocabulary);
            connection.commit();
            putToCache(ownerId, () -> saved);
            return saved;
        } catch (DuplicateVocabularyException e) {
            connection.rollback();
            LOG.warn("Duplicate vocabulary name '{}' for {}", name, ownerId);
            throw e;
        } catch (DaoException | DataAccessException e) {
            LOG.error("Error while creating a new vocabulary", e);
            connection.rollback();
            return null;
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean hasVocabulary(OwnerId ownerId, int vocabId) throws VocabNotFoundException {
        return userVocabsCache.getOrDefault(ownerId, Collections.emptyList())
                .stream()
                .anyMatch(vocabulary -> vocabulary.getVocabId() == vocabId)
                || vocabularyDao.hasAccess(ownerId, vocabId);
    }

    @Override
    public Vocabulary getVocabulary(OwnerId ownerId, int vocabId) {
        return findVocab(ownerId, vocabId);
    }

    @Override
    public boolean renameVocabulary(OwnerId ownerId, int vocabId, String newName) {
        // verify exists
        Vocabulary vocabulary = findVocab(ownerId, vocabId);
        if (Objects.equals(vocabulary.getName(), newName)) {
            return true;
        }
        // do modification
        try {
            connection.open();
            Vocabulary renamed = vocabularyDao.rename(ownerId, vocabId, newName);
            connection.commit();
            putToCache(ownerId, () -> renamed);
        } catch (DuplicateVocabularyException e) {
            connection.rollback();
            LOG.warn("Vocabulary name '{}' has been already taken for {}", newName, ownerId);
            throw e;
        } catch (DaoException e) {
            LOG.error("Error while renaming vocabulary, id = {}", vocabId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean changeVocabularyPicture(OwnerId ownerId, int vocabId, String newPictureUrl) {
        // verify exists
        Vocabulary vocabulary = findVocab(ownerId, vocabId);
        if (Objects.equals(vocabulary.getPictureUrl(), newPictureUrl)) {
            return true;
        }
        try {
            connection.open();
            vocabularyDao.updatePicture(vocabId, newPictureUrl);
            connection.commit();
            vocabulary.setPictureUrl(newPictureUrl); // should update pic url in cache
        } catch (DaoException e) {
            LOG.error("Error while changing vocabulary picture, id = {}", vocabId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean updateSharing(OwnerId ownerId, int vocabId, boolean isShared) {
        // verify exists
        Vocabulary vocabulary = findVocab(ownerId, vocabId);
        if (Objects.equals(vocabulary.isShared(), isShared)) {
            return true;
        }
        // do modifications
        try {
            connection.open();
            Vocabulary updated = vocabularyDao.updateIsShared(vocabId, isShared);
            connection.commit();
            putToCache(ownerId, () -> updated);
        } catch (DaoException e) {
            LOG.error("Error while updating vocabulary, id = {}", vocabId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public boolean deleteVocabulary(OwnerId ownerId, int vocabId) {
        // verify exists
        Vocabulary vocabulary = findVocab(ownerId, vocabId);
        if (vocabulary.getWordsTotal() > 0) {
            throw new DictionaryServiceException("Cannot delete vocabulary with words (not empty)");
        }
        // do action
        try {
            connection.open();
            vocabularyDao.deleteVocabularyById(vocabId);
            connection.commit();
            userVocabsCache.get(ownerId)
                    .remove(vocabulary);
        } catch (DaoException | DataAccessException e) {
            connection.rollback();
            LOG.error("Error while removing vocabulary by id = {}", vocabId, e);
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public List<Word> getWords(OwnerId ownerId, int vocabId) {
        if (wordsInVocabularyCache.containsKey(vocabId)) {
            List<Word> wordsInVocab = wordsInVocabularyCache.get(vocabId);
            if (!CollectionUtils.isEmpty(wordsInVocab)) {
                return wordsInVocab;
            }
        }
        List<Word> wordsInVocabulary;
        try {
            connection.open();
            Set<Integer> wordsRefs = vocabularyDao.getWordIds(findVocab(ownerId, vocabId).getVocabId());
            wordsInVocabulary = wordDao.findAllByIds(wordsRefs);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading all words for vocabulary id = {}", vocabId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }

        wordsInVocabularyCache.put(vocabId, wordsInVocabulary);

        return List.copyOf(wordsInVocabulary);
    }

    @Override
    public List<Card> getCards(OwnerId ownerId, int vocabId) {
        Vocabulary vocabulary = findVocab(ownerId, vocabId);

        String key = String.join(":", ownerId.ownerId(), String.valueOf(vocabId));

        if (cardsCache.containsKey(key)) {
            List<Card> cards = cardsCache.get(key);
            return List.copyOf(cards);
        }

        List<Card> cards;
        try {
            connection.open();
            // vocab words
            List<Word> wordsHeadlines = cardHeadlineDao.getWordsHeadlines(vocabulary.getVocabId());
            int[] wordIds = wordsHeadlines.stream().mapToInt(Word::getId).toArray();
            // user progress
            Map<Integer, Progress> progress = progressDao.selectByWordIds(ownerId, vocabId, wordIds)
                    .stream()
                    .collect(Collectors.toMap(Progress::getWordId, Function.identity()));
            connection.commit();

            // to cards with progress
            cards = wordsHeadlines
                    .stream()
                    .map(word -> {
                        Card card = new Card();
                        card.setVocabId(vocabId);
                        card.setWord(word);
                        card.setProgress(progress.getOrDefault(word.getId(), Progress.ofNullProgress(vocabId, word.getId())));
                        return card;
                    })
                    .collect(Collectors.toCollection(ArrayList::new)); // modifiable list

            wordsInVocabularyCache.put(vocabId, wordsHeadlines);

        } catch (DaoException e) {
            LOG.error("Error while loading all cards in vocabulary by id = {}", vocabId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        cardsCache.put(key, cards);

        return List.copyOf(cards);
    }

    @Override
    public List<Progress> getProgress(OwnerId ownerId, int vocabId) {
        List<Progress> cards;
        try {
            connection.open();
            Set<Integer> wordIds = vocabularyDao.getWordIds(findVocab(ownerId, vocabId).getVocabId());
            int[] array = wordIds.stream().mapToInt(Number::intValue).toArray();
            cards = progressDao.selectByWordIds(ownerId, vocabId, array);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while loading user cards progress in vocabulary by id = {}", vocabId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
        return cards;
    }

    @Override
    public List<FlashCard> pickFlashCards(OwnerId ownerId, int vocabId, int limit) {

        LOG.info("Getting cards for exercise from for user = {}, vocab id = {}", ownerId, vocabId);

        // build cache key, for example user-temp:123
        String key = String.join(":", ownerId.ownerId(), String.valueOf(vocabId));

        // cache is cleared when any card reached 100 score / learned status
        if (exerciseCache.containsKey(key)) {
            List<FlashCard> inCache = exerciseCache.get(key);
            if (inCache.size() >= limit) {
                return inCache.subList(0, limit);
            }
        }

        try {
            connection.open();
            // already in progress
            List<Progress> currentProgress = progressDao.pickForExercise(ownerId, vocabId, limit);
            List<FlashCard> allCards = cardHeadlineDao.getFlashCards(ownerId.ownerId(), vocabId);

            // map progress -> wordId
            Set<Integer> progressedExerciseIds = currentProgress.stream()
                    .map(Progress::getWordId)
                    .collect(Collectors.toSet());

            // step 1: take exercises that already have progress
            List<FlashCard> selected = allCards.stream()
                    .filter(e -> progressedExerciseIds.contains(e.wordId()))
                    .collect(Collectors.toList());

            // step 2: add more exercises if limit not reached
            if (selected.size() < limit) {
                int remaining = limit - selected.size();

                // take exercises not yet in progress
                List<FlashCard> newOnes = allCards.stream()
                        .filter(e -> !progressedExerciseIds.contains(e.wordId()))
                        .limit(remaining)
                        .toList();

                selected.addAll(newOnes);
            }
            connection.commit();

            exerciseCache.put(key, selected);
            if (CollectionUtils.isEmpty(selected)) {
                return Collections.emptyList();
            }
            return selected;
        } catch (DaoException e) {
            LOG.error("Error while loading cards for exercise by vocab id = {}", vocabId, e);
            return Collections.emptyList();
        } finally {
            connection.close();
        }
    }

    @Override
    public Score getProgressSummary(OwnerId ownerId, int vocabId) {
        try {
            Vocabulary vocabulary = findVocab(ownerId, vocabId);
            Score score = new Score();
            connection.open();
            Map<String, Integer> result = progressDao.getProgressSummary(ownerId, vocabulary.getVocabId());
            connection.commit();
            Set<String> statuses = result.keySet();
            for (String status : statuses) {
                score.withScoreCount(CardStatus.valueOf(status), result.get(status));
            }
            return score;
        } catch (DaoException e) {
            LOG.error("Error while getting total score summary for vocabulary, id = {}", vocabId, e);
            return null;
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean resetProgress(OwnerId ownerId, int vocabId, int wordId) {
        Progress progress = findProgressById(ownerId, vocabId, wordId);
        progress.setScore(0);
        progress.setStatus(CardStatus.TO_LEARN);
        LOG.info("Resetting score for a card id = {}", wordId);
        try {
            connection.open();
            progressDao.updateProgress(ownerId, progress);
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Error while resetting score for card, id = {}", wordId, e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    @Override
    public void saveProgress(OwnerId ownerId, int vocabId, int[] wordIds, int repetitions) {
        final int MAX_SCORE = 100;
        final int diff = MAX_SCORE / repetitions;

        int[] uniqueWordsIds = Arrays.stream(wordIds)
                .distinct()
                .toArray();

        // Generate full list of cards (creates missing, loads existing)
        List<Progress> allRecords = generateProgressRecords(ownerId, vocabId, uniqueWordsIds);

        List<Progress> updatedProgresses = new ArrayList<>();

        for (Progress progress : allRecords) {
            if (progress.getStatus() != CardStatus.LEARNT) {
                int score = progress.getScore() + diff;
                if (score >= MAX_SCORE) {
                    score = MAX_SCORE;
                    progress.setStatus(CardStatus.LEARNT);
                    String key = String.join(":", ownerId.ownerId(), String.valueOf(vocabId));
                    exerciseCache.remove(key);
                }
                progress.setScore(score);
                updatedProgresses.add(progress);
            }
        }

        if (updatedProgresses.isEmpty()) {
            return; // nothing to update
        }

        try {
            connection.open();
            progressDao.batchUpsertProgress(ownerId, updatedProgresses); // single operation
            connection.commit();
        } catch (DaoException e) {
            LOG.error("Failed to update progress for user={}, vocabId={}, words={}", ownerId, vocabId, Arrays.toString(wordIds), e);
            connection.rollback();
        } finally {
            connection.close();
        }
    }

    @Override
    public Word addToVocabulary(OwnerId ownerId, int vocabId, int wordId) {
        try {
            connection.open();

            boolean vocabulary = vocabularyDao.hasAccess(ownerId, vocabId);
            if (!vocabulary) {
                throw new DictionaryServiceException("Cannot modify vocabulary because of no access");
            }
            // add ref (safe operation)
            vocabularyDao.addWordsToVocabulary(vocabId, wordId);
            // reload word, when added
            if (wordsInVocabularyCache.containsKey(vocabId)) {
                List<Word> wordsInVocab = wordsInVocabularyCache.get(vocabId);
                boolean exists = wordsInVocab.stream()
                        .anyMatch(w -> Objects.equals(w.getId(), wordId));
                if (!exists) {
                    Word word = loadWordFromDb(wordId);
                    wordsInVocab.add(word);
                    String key = String.join(":", ownerId.ownerId(), String.valueOf(vocabId));
                    cardsCache.remove(key);
                    return word;
                }
            }
        } catch (DaoException e) {
            LOG.error("Error while adding a word id = {} to vocabulary = {}", wordId, vocabId, e);
            connection.rollback();
        } finally {
            connection.close();
        }
        return loadWordFromDb(wordId);
    }

    @Override
    public void removeFromVocabulary(OwnerId ownerId, int vocabId, int wordId) {
        try {
            connection.open();
            progressDao.delete(ownerId, vocabId, wordId);
            vocabularyDao.removeWordsFromVocabulary(vocabId, wordId);
            connection.commit();
            List<Word> wordsInVocab = wordsInVocabularyCache.get(vocabId);
            if (wordsInVocab != null) {
                wordsInVocab.removeIf(w -> Objects.equals(w.getId(), wordId));
            }

            String key = String.join(":", ownerId.ownerId(), String.valueOf(vocabId));
            List<Card> cards = cardsCache.get(key);
            cards.removeIf(c -> Objects.equals(c.getWordId(), wordId));
        } catch (DaoException e) {
            LOG.error("Error while removing a word id = {} from vocabulary = {}", wordId, vocabId, e);
            connection.rollback();
        } finally {
            connection.close();
        }
    }

    @Override
    public List<VocabularySummary> findVocabularySummaries(OwnersId ownersId) {
        if (ownersId.ownerIds().isEmpty()) {
            return List.of();
        }
        return vocabularyDao.findVocabularySummariesByType(ownersId);
    }

    private Vocabulary findVocab(OwnerId ownerId, int vocabId) {
        return userVocabsCache.getOrDefault(ownerId, Collections.emptyList()).stream()
                .filter(vocabulary -> vocabulary.getVocabId() == vocabId)
                .findAny()
                .orElseGet(() -> loadVocabFromDb(ownerId, vocabId));
    }

    private Vocabulary loadVocabFromDb(OwnerId ownerId, int vocabId) {
        Vocabulary vocabulary;
        try {
            vocabulary = vocabularyDao.selectById(vocabId)
                    .orElseThrow(() -> new VocabNotFoundException("Vocabulary with id = " + vocabId + " not found for owner id = " + ownerId));
        } catch (DataAccessException e) {
            throw new DictionaryServiceException();
        }
        putToCache(ownerId, () -> vocabulary);
        return vocabulary;
    }

    private Word findWordById(int vocabId, int wordId) {
        return wordsInVocabularyCache.get(vocabId)
                .stream()
                .filter(word -> word.getId() == wordId)
                .findFirst()
                .orElseGet(() -> loadWordFromDb(wordId));
    }

    private Word loadWordFromDb(int wordId) {
        Word word;
        try {
            word = wordDao.findById(wordId);
        } catch (DaoException e) {
            LOG.error("Error while loading a word by id = {}", wordId, e);
            throw new DictionaryServiceException();
        }
        if (word == null) {
            throw new WordNotFoundException();
        }
        return word;
    }

    private Progress findProgressById(OwnerId ownerId, int vocabId, int wordId) {
        String key = String.join(":", ownerId.ownerId(), String.valueOf(vocabId));

        return Optional.ofNullable(cardsCache.get(key))
                .flatMap(cards -> cards.stream()
                        .filter(card -> card.getWordId() == wordId)
                        .findFirst()
                        .map(card -> (Progress) card.getProgress()))
                .orElseGet(() -> loadProgressFromDb(ownerId, vocabId, wordId));
    }

    private Progress loadProgressFromDb(OwnerId ownerId, int vocabId, int wordId) {
        Progress progress;
        try {
            progress = progressDao.selectById(ownerId, vocabId, wordId);
        } catch (DaoException e) {
            LOG.error("Error while getting progress for a word id = {}", wordId, e);
            throw new DictionaryServiceException();
        }
        return progress;
    }

    private List<Progress> generateProgressRecords(OwnerId ownerId, int vocabId, int... wordIds) {

        try {
            connection.open();

            // Step 1: Load existing cards
            List<Progress> existingProgresses = progressDao.selectByWordIds(ownerId, vocabId, wordIds);
            Set<Integer> existingWordIds = existingProgresses.stream()
                    .map(Progress::getWordId)
                    .collect(Collectors.toSet());

            // Step 2: Determine missing
            Set<Integer> missingWordIds = Arrays.stream(wordIds).boxed().collect(Collectors.toCollection(TreeSet::new));
            missingWordIds.removeAll(existingWordIds);

            List<Progress> allProgresses = new ArrayList<>(existingProgresses);

            // Step 3: Prepare Card beans for missing and insert
            if (!missingWordIds.isEmpty()) {
                List<Progress> cardsToInsert = missingWordIds.stream()
                        .map(wordId -> {
                            Progress progress = new Progress();
                            progress.setVocabId(vocabId);
                            progress.setWordId(wordId);
                            progress.setStatus(CardStatus.TO_LEARN);
                            return progress;
                        })
                        .collect(Collectors.toList());

                if (cardsToInsert.size() == 1) {
                    Progress inserted = progressDao.addRecord(ownerId, cardsToInsert.getFirst());
                    allProgresses.add(inserted);
                } else {
                    progressDao.addRecords(ownerId, cardsToInsert);
                    // Fetch newly inserted cards back
                    int[] insertedWordIds = cardsToInsert.stream()
                            .mapToInt(Progress::getWordId)
                            .toArray();
                    List<Progress> newProgresses = progressDao.selectByWordIds(ownerId, vocabId, insertedWordIds);
                    allProgresses.addAll(newProgresses);
                }
            }

            connection.commit();
            return allProgresses;

        } catch (DaoException e) {
            LOG.error("Error while generating cards by vocabId = {}", vocabId, e);
            connection.rollback();
            return Collections.emptyList();
        } finally {
            connection.close();
        }
    }

    private void putToCache(OwnerId ownerId, Supplier<Vocabulary> vocabularySupplier) {
        Vocabulary newVocab = vocabularySupplier.get();
        userVocabsCache.compute(ownerId, (key, existingList) -> {
            if (existingList == null) {
                existingList = new ArrayList<>();
            } else {
                // Remove old entry with the same vocabId if it exists
                existingList.removeIf(v -> v.getVocabId() == newVocab.getVocabId());
            }
            existingList.add(newVocab);
            // Sort by updateTime descending (most recent first)
            existingList
                    .sort(Comparator.comparing(Vocabulary::getUpdateTime)
                            .reversed());
            return existingList;
        });
    }

}