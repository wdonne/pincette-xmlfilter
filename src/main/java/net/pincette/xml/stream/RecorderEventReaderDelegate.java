package net.pincette.xml.stream;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Records and replays events. If while replaying the recorded events are consumed, the filter
 * continues to read from the delegate.
 *
 * @author Werner Donné
 */
public class RecorderEventReaderDelegate extends EventReaderDelegateBase {
  private final List<XMLEvent> buffer = new ArrayList<>();
  private boolean recording = false;
  private int replayPosition = 0;

  public RecorderEventReaderDelegate() {}

  public RecorderEventReaderDelegate(final XMLEventReader reader) {
    super(reader);
  }

  public void clear() {
    buffer.clear();
    replayPosition = 0;
  }

  @Override
  public boolean hasNext() {
    return replayPosition < buffer.size() || getParent().hasNext();
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    final XMLEvent event =
        replayPosition < buffer.size() ? buffer.get(replayPosition++) : getParent().nextEvent();

    if (recording) {
      buffer.add(event);
      ++replayPosition;
    }

    setCurrentEvent(event);

    return event;
  }

  @Override
  public XMLEvent peek() throws XMLStreamException {
    return replayPosition < buffer.size() ? buffer.get(replayPosition) : getParent().peek();
  }

  public void replay() {
    if (!recording) {
      replayPosition = 0;
    }
  }

  public boolean replayDone() {
    return replayPosition == buffer.size();
  }

  public void stopRecording() {
    recording = false;
  }

  public void startRecording() {
    recording = true;
  }
}
