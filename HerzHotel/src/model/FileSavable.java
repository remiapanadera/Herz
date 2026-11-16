
package model;

import java.io.IOException;

public interface FileSavable {

    // Saves an object's information to a file.
    void saveToFile(String filePath) throws IOException;

    // Loads an object's information from a file.
    void loadFromFile(String filePath) throws IOException;
}
