package org.thuantn.desktopsearch.scanner;

import java.nio.file.Path;
import java.time.Instant;

public record FileMetadata(

	Path path,
	String fileName,
	String extension,
	long size,
	Instant createdAt,
	Instant modifiedAt

) {
}
