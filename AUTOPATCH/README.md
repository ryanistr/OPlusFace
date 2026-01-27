# AutoPatcher

- Patches **`services.jar`** and/or **`SystemUI.apk`**
- Uses **APKEditor.jar** under the hood
- Applies predefined patches located in the `faceunlock/` directory
- Optional debug mode for verbose logs (incase SystemUI wont compile cz it needs additional treatments like removing some duplicate/invalid resources)

---

## Requirements

- Linux (or WSL)
- Java

## Usage

### Patch `services.jar`

```bash
./patchFU -j services.jar
```

### Patch `SystemUI.apk`

```bash
./patchFU -a SystemUI.apk
```

You must provide the target file path as the argument.

---

## Debug mode

To enable verbose logging, create an empty file named `.debug` in the current directory:

```bash
touch .debug
```

Remove it to disable debug output.

---

## Notes / Warnings

- Always **back up your original files** before patching incase it gets cooked
- Results may vary depending on ROM version and Android release

---

## Troubleshooting

- Make sure Java is installed and accessible (`java -version`)
- Run with debug mode enabled if something fails (SystemUI/services unable to compile properly or smth)
