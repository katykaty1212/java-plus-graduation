package ru.practicum.ewm.stats.avro;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

public class EventSimilarityAvroDeserializer implements Deserializer<EventSimilarityAvro> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public EventSimilarityAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            DatumReader<EventSimilarityAvro> reader = new SpecificDatumReader<>(EventSimilarityAvro.getClassSchema());
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(data), null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException("Avro deserialization failed for topic " + topic, e);
        }
    }

    @Override
    public void close() {
    }
}