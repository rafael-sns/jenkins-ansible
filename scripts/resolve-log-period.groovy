import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Normalizes the LOG_PERIOD job parameter into ansible variables.
 *
 * @param logPeriodParam value from params.LOG_PERIOD (Active Choices or native Choice)
 * @return map with keys logPeriod ('today'|'yesterday'|'total') and logDate ('YYYY-MM-DD' or '')
 */
def call(String logPeriodParam) {
    def normalized = (logPeriodParam ?: '').trim().toUpperCase()
    def today = LocalDate.now()
    def yesterday = today.minusDays(1)
    def dateFormatter = DateTimeFormatter.ofPattern('yyyy-MM-dd')

    if (normalized.startsWith('1 -') || normalized == 'TODAY') {
        return [
            logPeriod: 'today',
            logDate  : today.format(dateFormatter),
        ]
    }

    if (normalized.startsWith('2 -') || normalized == 'YESTERDAY') {
        return [
            logPeriod: 'yesterday',
            logDate  : yesterday.format(dateFormatter),
        ]
    }

    if (normalized.startsWith('3 -') || normalized == 'TOTAL') {
        return [
            logPeriod: 'total',
            logDate  : '',
        ]
    }

    error("LOG_PERIOD inválido: '${logPeriodParam}'. Use TODAY, YESTERDAY, TOTAL ou opções Active Choices (1/2/3).")
}

return this
