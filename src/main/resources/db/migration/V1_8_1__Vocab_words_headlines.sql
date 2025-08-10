CREATE OR REPLACE VIEW vocab_words_headlines AS
(
SELECT vhw.vocab_id,
       vhw.word_ref                                                 AS word_id,
       w.word,
       w.part_of_speech,
       w.transcription,
       w.meaning,
       array_remove(array_agg(DISTINCT in_context.example), NULL)   AS card_sentences,
       array_remove(array_agg(DISTINCT collocations.example), NULL) AS card_collocations
FROM vocab_has_words vhw
         JOIN words w ON vhw.word_ref = w.id
         LEFT JOIN
     in_context ON w.id = in_context.word_id
         LEFT JOIN
     collocations ON w.id = collocations.word_id
GROUP BY vhw.vocab_id, vhw.word_ref, w.word, w.part_of_speech, w.transcription, w.meaning);
