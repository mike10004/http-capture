package io.github.mike10004.debexam;

import java.io.File;
import java.io.IOException;

public interface DebExaminer {

    DebExamination examine(File debFile) throws IOException;

}
