/* Regression probe against MuPDF's actual index_load implementation.
 * Compile with -DMUPDF_CFF_SOURCE='"/absolute/path/source/fitz/subset-cff.c"'.
 * The exception stub isolates this parser routine; it is not an end-to-end PDF test.
 */
#include <setjmp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include MUPDF_CFF_SOURCE

static jmp_buf exception;
FZ_NORETURN void fz_throw(fz_context *ctx, int code, const char *fmt, ...)
{
    (void)ctx; (void)code; (void)fmt;
    longjmp(exception, 1);
}

int main(int argc, char **argv)
{
    if (argc != 2) return 64;
    unsigned char *bytes = calloc(28, 1);
    if (!bytes) return 70;
    bytes[21] = 1; /* one item */
    bytes[22] = 1; /* one-byte offsets */
    bytes[23] = 1;
    bytes[24] = 2;
    int malformed = strcmp(argv[1], "valid") != 0;
    if (strcmp(argv[1], "extent") == 0) bytes[24] = 5;
    if (strcmp(argv[1], "offsets") == 0) {
        bytes[21] = 6;
        memset(bytes + 23, 1, 5);
    }
    index_t index = {0};
    if (setjmp(exception)) {
        free(bytes);
        puts("rejected");
        return malformed ? 0 : 2;
    }
    unsigned int end = index_load(NULL, &index, bytes, 28, 20);
    free(bytes);
    printf("accepted end=%u\n", end);
    return malformed ? 3 : 0;
}
