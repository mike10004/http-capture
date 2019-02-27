package io.github.mike10004.debexam;

import java.io.IOException;

public interface DebExamination extends java.io.Closeable {

    DebModel getModel() throws IOException;

}
