# AI Configuration Guide

The AI Ops Engine supports **real AI-powered analysis** using Large Language Models (LLMs) for:
- **Incident Classification**: Automatically categorizing data quality incidents
- **Root Cause Analysis**: Generating intelligent explanations of what went wrong
- **Remediation Recommendations**: Providing actionable steps to fix issues

## Quick Answer: Do I Need API Keys?

**NO, you don't need API keys!** The system works perfectly fine without any API keys. It automatically uses rule-based logic (templates) for diagnoses. AI is **disabled by default** and costs **$0**.

**To enable AI (optional):** Get your own API key from OpenAI or Anthropic and configure it (see below).

## Supported AI Providers

### 1. OpenAI (GPT-4, GPT-3.5, GPT-4o-mini)
- **Models**: `gpt-4`, `gpt-4-turbo`, `gpt-3.5-turbo`, `gpt-4o-mini` (default)
- **Cost**: Pay-per-use, very affordable with `gpt-4o-mini`
- **Setup**: Get API key from https://platform.openai.com/api-keys

### 2. Anthropic (Claude 3)
- **Models**: `claude-3-opus`, `claude-3-sonnet`, `claude-3-haiku-20240307` (default)
- **Cost**: Pay-per-use, `claude-3-haiku` is very cost-effective
- **Setup**: Get API key from https://console.anthropic.com/

### 3. Rule-Based Fallback (Default)
- **No API key required**
- **Always available** as a fallback
- Uses deterministic, template-based logic

## Configuration

### Option 1: Environment Variables (Recommended)

Set these environment variables before starting the service:

**For OpenAI:**
```bash
export AI_PROVIDER=openai
export AI_OPENAI_ENABLED=true
export AI_OPENAI_API_KEY=sk-your-api-key-here
export AI_OPENAI_MODEL=gpt-4o-mini  # Optional, defaults to gpt-4o-mini
```

**For Anthropic:**
```bash
export AI_PROVIDER=anthropic
export AI_ANTHROPIC_ENABLED=true
export AI_ANTHROPIC_API_KEY=sk-ant-your-api-key-here
export AI_ANTHROPIC_MODEL=claude-3-haiku-20240307  # Optional
```

**Auto-select (tries OpenAI first, then Anthropic, then fallback):**
```bash
export AI_PROVIDER=auto  # This is the default
export AI_OPENAI_ENABLED=true
export AI_OPENAI_API_KEY=sk-your-key-here
```

### Option 2: Docker Compose

Add to your `docker-compose.yml`:

```yaml
services:
  ai-ops-engine:
    environment:
      - AI_PROVIDER=openai
      - AI_OPENAI_ENABLED=true
      - AI_OPENAI_API_KEY=${OPENAI_API_KEY}
      - AI_OPENAI_MODEL=gpt-4o-mini
```

Then create a `.env` file (don't commit it!):
```bash
OPENAI_API_KEY=sk-your-api-key-here
```

### Option 3: Application Configuration

Edit `ai-ops-engine/src/main/resources/application.yml`:

```yaml
ai:
  provider: openai  # or "anthropic" or "auto"
  openai:
    enabled: true
    api-key: your-api-key-here
    model: gpt-4o-mini
  anthropic:
    enabled: false
    api-key: your-api-key-here
    model: claude-3-haiku-20240307
```

## How It Works

1. **When a data quality check fails**, the AI Ops Engine collects related failures
2. **After 3+ related failures**, it creates an incident
3. **The AI service analyzes** the failures and generates:
   - Classification (e.g., "DATA_INGESTION_FAILURE")
   - Root cause explanation (e.g., "Kafka consumer lag indicates upstream pipeline disruption...")
   - Remediation steps (e.g., "1. Check Kafka consumer lag...")
4. **If AI is unavailable**, it automatically falls back to rule-based logic

## Testing Without API Keys

The system works perfectly fine without API keys! It will automatically use the rule-based fallback service, which provides deterministic, template-based explanations.

## Cost Considerations

- **OpenAI GPT-4o-mini**: ~$0.15 per 1M input tokens, ~$0.60 per 1M output tokens
- **Anthropic Claude 3 Haiku**: ~$0.25 per 1M input tokens, ~$1.00 per 1M output tokens
- **Typical incident analysis**: ~500 tokens total = **$0.0003 - $0.0005 per incident**

For a platform processing 1000 incidents/day, that's approximately **$0.30 - $0.50 per day**.

## Security Best Practices

1. **Never commit API keys** to version control
2. Use environment variables or secrets management (Kubernetes secrets, AWS Secrets Manager, etc.)
3. Rotate API keys regularly
4. Monitor API usage to detect anomalies

## Troubleshooting

**AI not working?**
- Check logs: `docker-compose logs ai-ops-engine | grep -i "ai provider"`
- Verify API key is set correctly
- Check network connectivity to API endpoints
- System will automatically fall back to rule-based if AI fails

**Want to force rule-based?**
```bash
export AI_PROVIDER=rulebased
# or just don't set any API keys
```

## Example Output

**With AI (OpenAI GPT-4o-mini):**
```
Root Cause: Analysis of the failed data quality checks reveals a significant 
deviation in row count for table 'daily_revenue'. The actual row count of 0 
compared to the expected threshold suggests a complete data ingestion pipeline 
failure. This pattern, combined with the timestamp showing no recent data 
arrivals, indicates the Kafka consumer in the ingestion service has likely 
stopped processing events or the upstream event generator has ceased producing 
data. The correlation with multiple consecutive failures points to a systemic 
issue rather than transient network problems.

Remediations:
1. Verify the ingestion service is running and check its health endpoint
2. Inspect Kafka consumer lag metrics to identify if events are queuing
3. Review ingestion service logs for connection errors or processing exceptions
4. Confirm the event generator service is operational and producing events
5. Check network connectivity between Kafka, ingestion service, and event generator
```

**Without AI (Rule-based):**
```
Root Cause: Root cause analysis indicates a significant deviation in row count 
for table 'daily_revenue'. This suggests either data ingestion pipeline 
disruption or upstream source issues.

Remediations:
1. Check Kafka consumer lag and ingestion service health
2. Verify upstream event generator is operational
3. Review network connectivity between services
```
