import 'package:flutter/material.dart';

class PricingPage extends StatefulWidget {
  const PricingPage({super.key});

  @override
  State<PricingPage> createState() => _PricingPageState();
}

class _PricingPageState extends State<PricingPage> {
  String? _hoveredCard;

  Widget _buildPlanCard(
    BuildContext context, {
    required String title,
    required String price,
    String? period,
    required List<String> features,
    required Color bgColor,
    required bool isPrimary,
    required String buttonText,
  }) {
    return Card(
      elevation: isPrimary ? 8 : 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: isPrimary
            ? BorderSide(
                color: Theme.of(context).colorScheme.primary,
                width: 2,
              )
            : BorderSide.none,
      ),
      child: Container(
        width: 280,
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 16),
            Row(
              crossAxisAlignment: CrossAxisAlignment.baseline,
              textBaseline: TextBaseline.alphabetic,
              children: [
                Text(
                  price,
                  style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                ),
                if (period != null)
                  Text(
                    period,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
              ],
            ),
            const SizedBox(height: 24),
            ...features.map(
              (feature) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Row(
                  children: [
                    Icon(
                      Icons.check_circle,
                      color: isPrimary
                          ? Theme.of(context).colorScheme.primary
                          : Colors.green,
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Expanded(child: Text(feature)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: title!='Free'? ElevatedButton(
                onPressed: () {},
                style: ElevatedButton.styleFrom(
                  backgroundColor: isPrimary
                      ? Theme.of(context).colorScheme.primary
                      : Colors.white,
                  foregroundColor: isPrimary ? Colors.white : Colors.black87,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                    side: isPrimary
                        ? BorderSide.none
                        : BorderSide(color: Colors.grey[300]!),
                  ),
                  elevation: isPrimary ? 4 : 0,
                ),
                child: Text(buttonText),
              ): const SizedBox.shrink()
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Pricing Plans'),
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
                    Theme.of(context).colorScheme.primary,
                    Theme.of(context).colorScheme.tertiary,
                  ],
                ),
              ),
              child: Column(
                children: [
                  Text(
                    'Choose Your Plan',
                    style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Unlock the full potential of AI-assisted coding',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: Colors.white,
                        ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 30),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: LayoutBuilder(
                builder: (context, constraints) {
                  // Determine if we should stack the cards (when width is limited)
                  final bool shouldStack = constraints.maxWidth < 900;
                  
                  // Define the cards to be displayed
                  final List<Widget> cards = [
                    _buildCardWithHover(
                      context,
                      'free',
                      'Free',
                      '\$0',
                      null,
                      [
                        'Limited queries/month',
                        'Small project size',
                        'Basic support',
                        'Community access',
                      ],
                      Colors.grey[100]!,
                      'Start Free',
                      shouldStack,
                    ),
                    _buildCardWithHover(
                      context,
                      'team',
                      'Team',
                      '\$199.99',
                      '/month/user',
                      [
                        'Multi-user collaboration',
                        'Shared AI insights',
                        'API integrations',
                        'Priority support',
                        'Advanced analytics'
                      ],
                      Colors.blue[50]!,
                      'Choose Team',
                      shouldStack,
                    ),
                    _buildCardWithHover(
                      context,
                      'pro',
                      'Pro',
                      '\$399.99',
                      '/month',
                      [
                        'Unlimited queries',
                        'Advanced AI assistance',
                        'Priority support',
                        'Large project size',
                        'Extended code history'
                      ],
                      Colors.blue[50]!,
                      'Get Pro',
                      shouldStack,
                    ),
                    _buildCardWithHover(
                      context,
                      'enterprise',
                      'Enterprise',
                      'Custom',
                      null,
                      [
                        'On-prem deployment',
                        'Dedicated AI models',
                        'Full customization',
                        'SLA guarantees',
                        'Training & integration'
                      ],
                      Colors.blueGrey[50]!,
                      'Contact Sales',
                      shouldStack,
                    ),
                  ];

                  // If we should stack, use a Stack widget with positioned cards
                  if (shouldStack) {
                    return SizedBox(
                      height: 400, // Height for the stack area
                      child: Stack(
                        clipBehavior: Clip.none,
                        alignment: Alignment.center,
                        children: cards,
                      ),
                    );
                  }
                  
                  // Otherwise, use a horizontal row
                  return SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: cards.map((card) => Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 8),
                        child: card,
                      )).toList(),
                    ),
                  );
                },
              ),
            ),
            const SizedBox(height: 60),
          ],
        ),
      ),
    );
  }

  Widget _buildCardWithHover(
    BuildContext context, 
    String cardId,
    String title,
    String price,
    String? period,
    List<String> features,
    Color bgColor,
    String buttonText,
    bool stacked,
  ) {
    final isHovered = _hoveredCard == cardId;

    // Calculate position for stacked cards
    Widget card = MouseRegion(
      onEnter: (_) => setState(() => _hoveredCard = cardId),
      onExit: (_) => setState(() => _hoveredCard = null),
      child: AnimatedScale(
        duration: const Duration(milliseconds: 200),
        scale: isHovered ? 1.05 : 1.0,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          transform: Matrix4.identity()
            ..translate(0.0, isHovered ? -8.0 : 0.0, isHovered ? 10.0 : 0.0),
          child: _buildPlanCard(
            context,
            title: title,
            price: price,
            period: period,
            features: features,
            bgColor: isHovered
                ? Theme.of(context).colorScheme.primaryContainer
                : bgColor,
            isPrimary: isHovered,
            buttonText: buttonText,
          ),
        ),
      ),
    );

    // If stacked, wrap in positioned widget
    if (stacked) {
      // Base position - index the cards from right to left
      final int index = ['free', 'team', 'pro', 'enterprise'].indexOf(cardId);
      final double leftPosition = (index * 60.0); // Horizontal offset for stack effect
      
      // If hovered, bring to front by modifying position
      final double hoverAdjustment = isHovered ? -20 : 0;
      
      return Positioned(
        left: leftPosition + hoverAdjustment,
        child: card,
      );
    }

    return card;
  }
}