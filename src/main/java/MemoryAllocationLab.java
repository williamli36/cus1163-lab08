import java.io.*;
import java.util.*;

public class MemoryAllocationLab {

    static class MemoryBlock {
        int start;
        int size;
        String processName;

        public MemoryBlock(int start, int size, String processName) {
            this.start = start;
            this.size = size;
            this.processName = processName;
        }

        public boolean isFree() {
            return processName == null;
        }

        public int getEnd() {
            return start + size - 1;
        }
    }

    static int totalMemory;
    static ArrayList<MemoryBlock> memory;
    static int successfulAllocations = 0;
    static int failedAllocations = 0;

    public static void processRequests(String filename) {
        memory = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            totalMemory = Integer.parseInt(br.readLine().trim());
            System.out.println("Total Memory: " + totalMemory + " KB");
            System.out.println("----------------------------------------");
            System.out.println("\nProcessing requests...\n");

            memory.add(new MemoryBlock(0, totalMemory, null));

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(" ");

                if (parts[0].equalsIgnoreCase("REQUEST")) {
                    allocate(parts[1], Integer.parseInt(parts[2]));
                } else if (parts[0].equalsIgnoreCase("RELEASE")) {
                    deallocate(parts[1]);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private static void allocate(String processName, int size) {
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);

            if (block.isFree() && block.size >= size) {

                int remaining = block.size - size;

                block.processName = processName;
                block.size = size;

                if (remaining > 0) {
                    MemoryBlock newFree = new MemoryBlock(
                            block.start + size,
                            remaining,
                            null
                    );
                    memory.add(i + 1, newFree);
                }

                successfulAllocations++;
                System.out.println("REQUEST " + processName + " " + size + " KB → SUCCESS");
                return;
            }
        }

        failedAllocations++;
        System.out.println("REQUEST " + processName + " " + size + " KB → FAILED (Insufficient memory)");
    }

    private static void deallocate(String processName) {
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);

            if (!block.isFree() && block.processName.equals(processName)) {
                block.processName = null;
                System.out.println("RELEASE " + processName + " → SUCCESS");
                mergeAdjacentBlocks();
                return;
            }
        }

        System.out.println("RELEASE " + processName + " → FAILED (Process not found)");
    }

    private static void mergeAdjacentBlocks() {
        for (int i = 0; i < memory.size() - 1; i++) {
            MemoryBlock current = memory.get(i);
            MemoryBlock next = memory.get(i + 1);

            if (current.isFree() && next.isFree()) {
                current.size += next.size;
                memory.remove(i + 1);
                i--;
            }
        }
    }

    public static void displayStatistics() {
        System.out.println("\n========================================");
        System.out.println("Final Memory State");
        System.out.println("========================================");

        int blockNum = 1;
        for (MemoryBlock block : memory) {
            String status = block.isFree() ? "FREE" : block.processName;
            String allocated = block.isFree() ? "" : " - ALLOCATED";
            System.out.printf("Block %d: [%d-%d]%s%s (%d KB)%s\n",
                    blockNum++,
                    block.start,
                    block.getEnd(),
                    " ".repeat(Math.max(1, 10 - String.valueOf(block.getEnd()).length())),
                    status,
                    block.size,
                    allocated);
        }

        System.out.println("\n========================================");
        System.out.println("Memory Statistics");
        System.out.println("========================================");

        int allocatedMem = 0;
        int freeMem = 0;
        int numProcesses = 0;
        int numFreeBlocks = 0;
        int largestFree = 0;

        for (MemoryBlock block : memory) {
            if (block.isFree()) {
                freeMem += block.size;
                numFreeBlocks++;
                largestFree = Math.max(largestFree, block.size);
            } else {
                allocatedMem += block.size;
                numProcesses++;
            }
        }

        double allocatedPercent = (allocatedMem * 100.0) / totalMemory;
        double freePercent = (freeMem * 100.0) / totalMemory;
        double fragmentation = freeMem > 0 ?
                ((freeMem - largestFree) * 100.0) / freeMem : 0;

        System.out.printf("Total Memory:           %d KB\n", totalMemory);
        System.out.printf("Allocated Memory:       %d KB (%.2f%%)\n", allocatedMem, allocatedPercent);
        System.out.printf("Free Memory:            %d KB (%.2f%%)\n", freeMem, freePercent);
        System.out.printf("Number of Processes:    %d\n", numProcesses);
        System.out.printf("Number of Free Blocks:  %d\n", numFreeBlocks);
        System.out.printf("Largest Free Block:     %d KB\n", largestFree);
        System.out.printf("External Fragmentation: %.2f%%\n", fragmentation);

        System.out.println("\nSuccessful Allocations: " + successfulAllocations);
        System.out.println("Failed Allocations:     " + failedAllocations);
        System.out.println("========================================");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java MemoryAllocationLab <input_file>");
            System.out.println("Example: java MemoryAllocationLab memory_requests.txt");
            return;
        }

        System.out.println("========================================");
        System.out.println("Memory Allocation Simulator (First-Fit)");
        System.out.println("========================================\n");
        System.out.println("Reading from: " + args[0]);

        processRequests(args[0]);
        displayStatistics();
    }
}
