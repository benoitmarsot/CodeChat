import 'package:flutter/material.dart';

class AboutPage extends StatelessWidget {
  const AboutPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('About Augmentera'), // Changed title
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 40, horizontal: 20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    Theme.of(context).colorScheme.primaryContainer,
                    Theme.of(context).colorScheme.surfaceContainer,
                  ],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ),
              ),
              child: Column(
                children: [
                  // Augmentera Logo
                  Image.asset(
                    'assets/augmentera_logo.png', // Replace with your logo asset path
                    height: 100, // Adjust as needed
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Democratize AI with your authentic brand voice', // Blurb
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Augmentera makes advanced retrieval-augmented generation (RAG) accessible to any team — no need for expensive infrastructure, in-house AI experts, or custom engineering.', // Blurb
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: Colors.white,
                        ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 30),
            Padding(
              padding: const EdgeInsets.all(64),
              child: Column(children: [
                _buildSection(
                  context,
                  'How Augmentera Works', // Changed title
                  [
                    'Upload Your Knowledge Base – Augmentera processes and indexes your data, creating an intelligent understanding of your content.', // Changed text
                    'Ask Questions – Get immediate, context-aware responses tailored to your data.', // Changed text
                    'AI-Powered Insights – Automate content generation, improve customer support, and enhance decision-making with AI assistance.', // Changed text
                    'Integrate with Your Workflow – Works seamlessly with your existing systems and processes.', // Changed text
                  ],
                ),
                const SizedBox(height: 30),
                _buildSection(
                  context,
                  'Why Augmentera?', // Changed title
                  [
                    'Deep Content Understanding – Leverages AI-powered Retrieval-Augmented Generation (RAG) to provide answers tailored to your specific knowledge base.', // Changed text
                    'Boost Productivity – Reduce information retrieval time, accelerate onboarding, and enhance collaboration.', // Changed text
                    'Secure & Private – Designed to prioritize security, with optional on-premise deployment for enterprises.',
                    'Future-Ready – Continuously evolving to support predictive analytics, AI-enhanced automation, and real-time insights.', // Changed text
                  ],
                ),
                const SizedBox(height: 30),
                Center(
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      vertical: 16.0,
                      horizontal: 24.0,
                    ),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.primaryContainer,
                      borderRadius: BorderRadius.circular(8.0),
                    ),
                    child: Text(
                      'Join the future of AI-powered knowledge management with Augmentera. Whether you\'re an individual or an enterprise team, Augmentera is here to supercharge your access to information and insights.', // Changed text
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                            fontWeight: FontWeight.bold,
                          ),
                    ),
                  ),
                ),
                const SizedBox(height: 40),
              ]),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSection(
      BuildContext context, String title, List<String> bulletPoints) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: Theme.of(context).textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.bold,
                color: Theme.of(context).colorScheme.primary,
              ),
        ),
        const SizedBox(height: 16),
        ...bulletPoints.map(
          (point) => Padding(
            padding: const EdgeInsets.only(bottom: 12.0),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(
                  Icons.arrow_right,
                  color: Theme.of(context).colorScheme.secondary,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    point,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
