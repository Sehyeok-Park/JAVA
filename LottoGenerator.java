import java.io.*;
import java.util.*;

/**
 * LottoGenerator 클래스
 * - 로또 번호 생성의 기본 기능(통계 기반 번호 생성, 빈도 계산, 파일 저장 등)을 제공하는 부모 클래스
 * - 자식 클래스에서 메서드 오버라이딩을 통해 다양한 번호 생성 전략을 구현할 수 있음
 */
public class LottoGenerator {
    // 로또 번호의 최소값, 최대값, 한 게임당 번호 개수, 파일명 상수
    protected static final int LOTTO_MIN = 1; // 로또 번호 최소값
    protected static final int LOTTO_MAX = 45; // 로또 번호 최대값
    protected static final int LOTTO_COUNT = 6; // 한 게임에 들어가는 번호 개수
    protected static final String RECENT_FILE = "recent_lotto_numbers.txt"; // 최근 당첨 번호 파일명
    protected static final String OUTPUT_FILE = "generated_lotto_numbers.txt"; // 생성 번호 저장 파일명

    // 번호별 출현 빈도를 저장하는 맵 (static: 모든 인스턴스가 공유)
    protected static Map<Integer, Integer> frequency = new HashMap<>();

    /**
     * 생성자: frequency 맵이 비어있으면 1~45까지 0으로 초기화
     */
    public LottoGenerator() {
        if (frequency.isEmpty()) {
            for (int i = LOTTO_MIN; i <= LOTTO_MAX; i++) {
                frequency.put(i, 0);
            }
        }
    }

    /**
     * 최근 당첨 번호 파일을 읽어 frequency 맵을 채움
     * 파일이 없으면 안내 메시지 출력
     */
    protected static void loadRecentNumbers() {
        // 빈도 맵 초기화
        for (int i = LOTTO_MIN; i <= LOTTO_MAX; i++) {
            frequency.put(i, 0);
        }
        File file = new File(RECENT_FILE);
        if (!file.exists()) {
            System.out.println("최근 당첨 번호 파일이 없습니다.");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            // 파일의 각 줄을 읽어서 번호별 빈도 누적
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 7) continue; // 잘못된 라인 무시
                for (String part : parts) {
                    int num = Integer.parseInt(part.trim());
                    frequency.put(num, frequency.get(num) + 1); // 빈도 누적
                }
            }
        } catch (IOException e) {
            System.out.println("파일 읽기 오류: " + e.getMessage());
        }
    }

    /**
     * 여러 게임을 생성하여 리스트로 반환
     * @param count 생성할 게임 수
     * @return 생성된 LottoGame 객체 리스트
     */
    public List<LottoGame> generateMultipleGames(int count) {
        List<LottoGame> games = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            games.add(generateWeightedGame());
        }
        return games;
    }

    /**
     * 통계 기반(가중치 기반)으로 6개 번호를 뽑아 LottoGame 객체로 반환
     * (자식 클래스에서 오버라이딩 가능)
     * @return 생성된 LottoGame 객체
     */
    protected LottoGame generateWeightedGame() {
        // 가장 많이 나온 번호의 빈도(가중치 계산 기준)
        int maxFreq = Collections.max(frequency.values());
        List<Integer> numbers = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        // 각 번호별로 가중치 계산 (출현 빈도가 적을수록 가중치가 높음)
        for (int i = LOTTO_MIN; i <= LOTTO_MAX; i++) {
            numbers.add(i);
            weights.add(maxFreq - frequency.get(i) + 1);
        }

        Set<Integer> mainNumbers = new TreeSet<>(); // 최종 선택된 번호 집합
        Random rand = new Random();
        List<Integer> tempNumbers = new ArrayList<>(numbers); // 후보 번호 리스트
        List<Integer> tempWeights = new ArrayList<>(weights); // 후보 번호별 가중치 리스트

        // 6개 번호가 뽑힐 때까지 반복
        while (mainNumbers.size() < LOTTO_COUNT) {
            int totalWeight = tempWeights.stream().mapToInt(Integer::intValue).sum(); // 전체 가중치 합
            int r = rand.nextInt(totalWeight); // 0 ~ totalWeight-1 중 랜덤
            int sum = 0;
            // 가중치 누적합을 이용해 랜덤하게 번호 선택
            for (int i = 0; i < tempNumbers.size(); i++) {
                sum += tempWeights.get(i);
                if (r < sum) {
                    int candidate = tempNumbers.get(i);
                    if (!mainNumbers.contains(candidate)) { // 중복 방지
                        mainNumbers.add(candidate); // 번호 추가
                        tempNumbers.remove(i); // 후보에서 제거
                        tempWeights.remove(i); // 가중치도 제거
                    }
                    break;
                }
            }
        }
        // 뽑은 번호로 LottoGame 객체 생성
        return new LottoGame(mainNumbers);
    }

    /**
     * 생성된 게임들을 파일에 저장
     * @param games 저장할 LottoGame 리스트
     */
    public void saveGames(List<LottoGame> games) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_FILE))) {
            for (LottoGame game : games) {
                bw.write(game.toString());
                bw.newLine();
            }
            System.out.println("생성된 번호가 파일에 저장되었습니다.");
        } catch (IOException e) {
            System.out.println("파일 저장 오류: " + e.getMessage());
        }
    }

    /**
     * 번호별 빈도 출력
     */
    public void printFrequencies() {
        System.out.println("\n번호별 빈도:");
        for (int i = LOTTO_MIN; i <= LOTTO_MAX; i++) {
            System.out.printf("%2d: %d회\n", i, frequency.get(i));
        }
    }

    /**
     * 번호별 빈도를 텍스트 막대그래프(아스키 아트)로 출력
     * 가장 많이 나온 번호가 103번이어도 자동 비율 조정
     */
    public void printFrequenciesBar() {
        System.out.println("\n번호별 빈도(막대그래프):");
        int max = Collections.max(frequency.values()); // 예: 103
        int maxBar = 50; // 막대 최대 길이(문자 수)
        for (int i = LOTTO_MIN; i <= LOTTO_MAX; i++) {
            int freq = frequency.get(i);
            // 막대 길이 계산 (최대 maxBar)
            int barLength = (max > 0) ? (int)Math.round((freq * maxBar * 1.0) / max) : 0;
            String bar = new String(new char[barLength]).replace('\0', '█');
            System.out.printf("%2d: %3d | %s\n", i, freq, bar);
        }
    }

    /**
     * 메인 메뉴: 사용자 입력을 받아 다양한 방식의 로또 번호를 생성
     */
    public static void main(String[] args) {
        // 최근 당첨 번호 파일에서 빈도 정보 로딩
        LottoGenerator.loadRecentNumbers();

        Scanner sc = new Scanner(System.in);
        // 각 기능별 생성기 객체 생성
        LottoGenerator basicGen = new LottoGenerator();
        IncludeLottoGenerator includeGen = new IncludeLottoGenerator();
        ExcludeLottoGenerator excludeGen = new ExcludeLottoGenerator();
        IncludeExcludeLottoGenerator includeExcludeGen = new IncludeExcludeLottoGenerator();

        // 메뉴 반복
        while (true) {
            System.out.println("\n===== 로또 번호 생성기 =====");
            System.out.println("1. 임의의 숫자 생성");
            System.out.println("2. 특정한 숫자 포함");
            System.out.println("3. 특정한 숫자 제외");
            System.out.println("4. 특정한 숫자 포함 + 특정한 숫자 제외");
            System.out.println("5. 번호별 빈도(숫자) 출력");
            System.out.println("6. 번호별 빈도(막대그래프) 출력");
            System.out.println("7. 종료");
            System.out.print("메뉴를 선택하세요: ");
            int menu;
            try {
                menu = Integer.parseInt(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.println("숫자를 올바르게 입력하세요.");
                continue;
            }
            if (menu == 1) {
                // 1. 임의의 숫자 생성 (부모 클래스 기본 기능)
                System.out.println("\n[임의의 숫자 생성]");
                List<LottoGame> games = basicGen.generateMultipleGames(5);
                for (int i = 0; i < games.size(); i++) {
                    System.out.printf("게임 %d: %s\n", i + 1, games.get(i));
                }
                basicGen.saveGames(games);
            } else if (menu == 2) {
                // 2. 특정 숫자 포함 (IncludeLottoGenerator 오버라이딩 기능)
                while (true) {
                    try {
                        System.out.print("포함할 숫자들을 공백으로 구분해서 입력: ");
                        String line = sc.nextLine().trim();
                        if (line.isEmpty()) {
                            System.out.println("입력이 없습니다. 다시 입력해주세요.");
                            continue;
                        }
                        String[] tokens = line.split("\\s+");
                        Set<Integer> mustInclude = new HashSet<>();
                        for (String token : tokens) {
                            if (!token.isEmpty()) mustInclude.add(Integer.parseInt(token));
                        }
                        includeGen.setMustInclude(mustInclude);
                        List<LottoGame> games = new ArrayList<>();
                        for (int i = 0; i < 5; i++) {
                            games.add(includeGen.generateWeightedGame());
                            System.out.printf("게임 %d: %s\n", i + 1, games.get(i));
                        }
                        includeGen.saveGames(games);
                        break;
                    } catch (Exception e) {
                        System.out.println("입력 오류: " + e.getMessage());
                        System.out.println("다시 입력해주세요.");
                    }
                }
            } else if (menu == 3) {
                // 3. 특정 숫자 제외 (ExcludeLottoGenerator 오버라이딩 기능)
                while (true) {
                    try {
                        System.out.print("제외할 숫자들을 공백으로 구분해서 입력: ");
                        String line = sc.nextLine().trim();
                        if (line.isEmpty()) {
                            System.out.println("입력이 없습니다. 다시 입력해주세요.");
                            continue;
                        }
                        String[] tokens = line.split("\\s+");
                        Set<Integer> mustExclude = new HashSet<>();
                        for (String token : tokens) {
                            if (!token.isEmpty()) mustExclude.add(Integer.parseInt(token));
                        }
                        excludeGen.setMustExclude(mustExclude);
                        List<LottoGame> games = new ArrayList<>();
                        for (int i = 0; i < 5; i++) {
                            games.add(excludeGen.generateWeightedGame());
                            System.out.printf("게임 %d: %s\n", i + 1, games.get(i));
                        }
                        excludeGen.saveGames(games);
                        break;
                    } catch (Exception e) {
                        System.out.println("입력 오류: " + e.getMessage());
                        System.out.println("다시 입력해주세요.");
                    }
                }
            } else if (menu == 4) {
                // 4. 특정 숫자 포함 + 제외 (IncludeExcludeLottoGenerator 오버라이딩 기능)
                while (true) {
                    try {
                        System.out.print("포함할 숫자들을 공백으로 구분해서 입력: ");
                        String line1 = sc.nextLine().trim();
                        if (line1.isEmpty()) {
                            System.out.println("입력이 없습니다. 다시 입력해주세요.");
                            continue;
                        }
                        String[] tokens1 = line1.split("\\s+");
                        Set<Integer> mustInclude = new HashSet<>();
                        for (String token : tokens1) {
                            if (!token.isEmpty()) mustInclude.add(Integer.parseInt(token));
                        }
                        System.out.print("제외할 숫자들을 공백으로 구분해서 입력: ");
                        String line2 = sc.nextLine().trim();
                        if (line2.isEmpty()) {
                            System.out.println("입력이 없습니다. 다시 입력해주세요.");
                            continue;
                        }
                        String[] tokens2 = line2.split("\\s+");
                        Set<Integer> mustExclude = new HashSet<>();
                        for (String token : tokens2) {
                            if (!token.isEmpty()) mustExclude.add(Integer.parseInt(token));
                        }
                        includeExcludeGen.setMustIncludeExclude(mustInclude, mustExclude);
                        List<LottoGame> games = new ArrayList<>();
                        for (int i = 0; i < 5; i++) {
                            games.add(includeExcludeGen.generateWeightedGame());
                            System.out.printf("게임 %d: %s\n", i + 1, games.get(i));
                        }
                        includeExcludeGen.saveGames(games);
                        break;
                    } catch (Exception e) {
                        System.out.println("입력 오류: " + e.getMessage());
                        System.out.println("다시 입력해주세요.");
                    }
                }
            } else if (menu == 5) {
                // 5. 번호별 빈도(숫자) 출력
                basicGen.printFrequencies();
            } else if (menu == 6) {
                // 6. 번호별 빈도(막대그래프) 출력
                basicGen.printFrequenciesBar();
            } else if (menu == 7) {
                // 7. 종료
                System.out.println("프로그램을 종료합니다.");
                break;
            } else {
                System.out.println("잘못된 입력입니다.");
            }
        }
        sc.close();
    }
}

/**
 * IncludeLottoGenerator 클래스
 * - 특정 숫자를 반드시 포함해서 번호를 생성하는 자식 클래스
 * - generateWeightedGame() 메서드를 오버라이딩하여 포함 숫자 기능 구현
 */
class IncludeLottoGenerator extends LottoGenerator {
    private Set<Integer> mustInclude = new HashSet<>();
    public void setMustInclude(Set<Integer> mustInclude) {
        this.mustInclude = new HashSet<>(mustInclude);
    }
    @Override
    protected LottoGame generateWeightedGame() {
        // 입력 검증: 포함 숫자가 6개 초과면 예외
        if (mustInclude.size() > LOTTO_COUNT) {
            throw new IllegalArgumentException("포함할 숫자는 최대 6개까지만 가능합니다.");
        }
        // 입력 검증: 1~45 범위 체크
        for (int n : mustInclude) {
            if (n < LOTTO_MIN || n > LOTTO_MAX) {
                throw new IllegalArgumentException("숫자는 1~45 사이여야 합니다: " + n);
            }
        }
        Set<Integer> result = new TreeSet<>(mustInclude); // 포함 숫자 미리 추가
        Random rand = new Random();
        int maxFreq = Collections.max(frequency.values());
        List<Integer> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        // 포함 숫자를 제외한 후보와 가중치 준비
        for (int i = LOTTO_MIN; i <= LOTTO_MAX; i++) {
            if (!result.contains(i)) {
                candidates.add(i);
                weights.add(maxFreq - frequency.get(i) + 1);
            }
        }
        // 나머지 번호를 통계 기반으로 추출
        while (result.size() < LOTTO_COUNT) {
            int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
            int r = rand.nextInt(totalWeight);
            int sum = 0;
            for (int i = 0; i < candidates.size(); i++) {
                sum += weights.get(i);
                if (r < sum) {
                    result.add(candidates.get(i));
                    candidates.remove(i);
                    weights.remove(i);
                    break;
                }
            }
        }
        return new LottoGame(result);
    }
}

/**
 * ExcludeLottoGenerator 클래스
 * - 특정 숫자를 반드시 제외해서 번호를 생성하는 자식 클래스
 * - generateWeightedGame() 메서드를 오버라이딩하여 제외 숫자 기능 구현
 */
class ExcludeLottoGenerator extends LottoGenerator {
    private Set<Integer> mustExclude = new HashSet<>();
    public void setMustExclude(Set<Integer> mustExclude) {
        this.mustExclude = new HashSet<>(mustExclude);
    }
    @Override
    protected LottoGame generateWeightedGame() {
        // 입력 검증: 제외 숫자가 너무 많으면 예외
        if (mustExclude.size() > LOTTO_MAX - LOTTO_COUNT) {
            throw new IllegalArgumentException("제외할 숫자가 너무 많습니다.");
        }
        // 입력 검증: 1~45 범위 체크
        for (int n : mustExclude) {
            if (n < LOTTO_MIN || n > LOTTO_MAX) {
                throw new IllegalArgumentException("숫자는 1~45 사이여야 합니다: " + n);
            }
        }
        Set<Integer> result = new TreeSet<>();
        Random rand = new Random();
        int maxFreq = Collections.max(frequency.values());
        List<Integer> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        // 제외 숫자를 뺀 후보와 가중치 준비
        for (int i = LOTTO_MIN; i <= LOTTO_MAX; i++) {
            if (!mustExclude.contains(i)) {
                candidates.add(i);
                weights.add(maxFreq - frequency.get(i) + 1);
            }
        }
        // 번호를 통계 기반으로 추출
        while (result.size() < LOTTO_COUNT) {
            int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
            int r = rand.nextInt(totalWeight);
            int sum = 0;
            for (int i = 0; i < candidates.size(); i++) {
                sum += weights.get(i);
                if (r < sum) {
                    result.add(candidates.get(i));
                    candidates.remove(i);
                    weights.remove(i);
                    break;
                }
            }
        }
        return new LottoGame(result);
    }
}

/**
 * IncludeExcludeLottoGenerator 클래스
 * - 특정 숫자를 반드시 포함하고, 또 다른 숫자를 반드시 제외해서 번호를 생성하는 자식 클래스
 * - generateWeightedGame() 메서드를 오버라이딩하여 포함+제외 기능 구현
 */
class IncludeExcludeLottoGenerator extends LottoGenerator {
    private Set<Integer> mustInclude = new HashSet<>();
    private Set<Integer> mustExclude = new HashSet<>();
    public void setMustIncludeExclude(Set<Integer> mustInclude, Set<Integer> mustExclude) {
        this.mustInclude = new HashSet<>(mustInclude);
        this.mustExclude = new HashSet<>(mustExclude);
    }
    @Override
    protected LottoGame generateWeightedGame() {
        // 입력 검증: 포함/제외 숫자가 겹치면 예외
        if (!Collections.disjoint(mustInclude, mustExclude)) {
            throw new IllegalArgumentException("포함과 제외 숫자가 겹칠 수 없습니다.");
        }
        // 입력 검증: 포함 숫자 개수, 제외 숫자 개수 체크
        if (mustInclude.size() > LOTTO_COUNT) {
            throw new IllegalArgumentException("포함할 숫자는 최대 6개까지만 가능합니다.");
        }
        if (mustExclude.size() > LOTTO_MAX - LOTTO_COUNT) {
            throw new IllegalArgumentException("제외할 숫자가 너무 많습니다.");
        }
        Set<Integer> result = new TreeSet<>(mustInclude); // 포함 숫자 미리 추가
        Random rand = new Random();
        int maxFreq = Collections.max(frequency.values());
        List<Integer> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        // 포함, 제외 숫자를 모두 뺀 후보와 가중치 준비
        for (int i = LOTTO_MIN; i <= LOTTO_MAX; i++) {
            if (!result.contains(i) && !mustExclude.contains(i)) {
                candidates.add(i);
                weights.add(maxFreq - frequency.get(i) + 1);
            }
        }
        // 나머지 번호를 통계 기반으로 추출
        while (result.size() < LOTTO_COUNT) {
            int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
            int r = rand.nextInt(totalWeight);
            int sum = 0;
            for (int i = 0; i < candidates.size(); i++) {
                sum += weights.get(i);
                if (r < sum) {
                    result.add(candidates.get(i));
                    candidates.remove(i);
                    weights.remove(i);
                    break;
                }
            }
        }
        return new LottoGame(result);
    }
}

/**
 * LottoGame 클래스
 * - 로또 한 게임(6개 번호 집합)을 표현하는 데이터 클래스
 * - 번호 집합을 저장하고, toString()으로 오름차순 출력
 */
class LottoGame {
    private Set<Integer> numbers; // 한 게임의 번호 집합

    /**
     * 생성자: 번호 집합을 받아 저장
     * @param numbers 6개 번호 집합
     */
    public LottoGame(Set<Integer> numbers) {
        this.numbers = new TreeSet<>(numbers); // 오름차순 저장
    }

    /**
     * 번호를 오름차순 정렬된 문자열로 반환
     * @return [1, 7, 14, 22, 33, 41] 형식의 문자열
     */
    @Override
    public String toString() {
        List<Integer> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        return sorted.toString();
    }
}