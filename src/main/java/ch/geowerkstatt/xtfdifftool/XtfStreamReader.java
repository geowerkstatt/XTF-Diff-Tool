package ch.geowerkstatt.xtfdifftool;

import ch.ehi.basics.settings.Settings;
import ch.interlis.iom.IomObject;
import ch.interlis.iox.EndTransferEvent;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.ObjectEvent;
import ch.interlis.iox_j.logging.LogEventFactory;
import ch.interlis.iox_j.utility.ReaderFactory;

import java.io.File;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A reader for INTERLIS transfer files.
 */
public final class XtfStreamReader implements AutoCloseable {
    private static final ReaderFactory READER_FACTORY = new ReaderFactory();

    private final IoxReader reader;
    private boolean initialized;

    /**
     * Creates a new reader for the INTERLIS transfer file.
     * @param xtfFile The file to read from.
     * @throws IoxException If an error occurs while creating the transfer file reader.
     */
    public XtfStreamReader(File xtfFile) throws IoxException {
        LogEventFactory logEventFactory = new LogEventFactory();
        Settings settings = new Settings();
        this.reader = READER_FACTORY.createReader(xtfFile, logEventFactory, settings);
    }

    /**
     * Reads the objects as a sequential stream.
     * Advancing the stream may throw an exception when reading invalid data.
     * @return A stream of objects contained in the xtf file.
     * @throws IllegalStateException If this method is called more than once.
     */
    public Stream<IomObject> readObjects() {
        if (initialized) {
            throw new IllegalStateException("readObjects() can only be called once");
        }
        initialized = true;

        return StreamSupport.stream(new XtfReaderSpliterator(), false);
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }

    /**
     * A sequential spliterator for reading objects from the surrounding {@link XtfStreamReader}.
     * Advancing the spliterator will read from the xtf reader.
     */
    private final class XtfReaderSpliterator implements Spliterator<IomObject> {
        @Override
        public boolean tryAdvance(Consumer<? super IomObject> action) {
            try {
                IoxEvent event = reader.read();
                while (event != null) {
                    switch (event) {
                        case ObjectEvent objectEvent -> {
                            action.accept(objectEvent.getIomObject());
                            return true;
                        }
                        case EndTransferEvent ignored -> {
                            return false;
                        }
                        default -> { }
                    }
                    event = reader.read();
                }

                throw new IllegalStateException("Unexpected end of file");
            } catch (IoxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Spliterator<IomObject> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return IMMUTABLE | NONNULL;
        }
    }
}
