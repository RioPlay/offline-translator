/* Exercise the actual MuPDF stream API; link to the tested native libraries. */
#include "mupdf/fitz.h"
#include <stdio.h>
#include <string.h>

int main(int argc, char **argv)
{
    if (argc != 2) return 64;
    int h = 1, pad = 0, skip = 0;
    int valid = strcmp(argv[1], "valid") == 0;
    if (strcmp(argv[1], "height") == 0) h = -1;
    if (strcmp(argv[1], "pad") == 0) pad = 2;
    if (strcmp(argv[1], "skip") == 0) skip = -1;
    fz_context *ctx = fz_new_context(NULL, NULL, FZ_STORE_UNLIMITED);
    if (!ctx) return 70;
    fz_stream *input = NULL, *output = NULL;
    int rejected = 0;
    unsigned char pixel = 42;
    fz_var(input); fz_var(output); fz_var(rejected);
    fz_try(ctx) {
        input = fz_open_memory(ctx, &pixel, 1);
        output = fz_unpack_stream(ctx, input, 8, 1, h, 1, 0, pad, skip);
        if (valid && fz_read_byte(ctx, output) != 42)
            fz_throw(ctx, FZ_ERROR_GENERIC, "valid pixel changed");
    }
    fz_always(ctx) {
        fz_drop_stream(ctx, output);
        fz_drop_stream(ctx, input);
    }
    fz_catch(ctx) { rejected = 1; }
    fz_drop_context(ctx);
    puts(rejected ? "rejected" : "accepted");
    return valid ? (rejected ? 2 : 0) : (rejected ? 0 : 3);
}
