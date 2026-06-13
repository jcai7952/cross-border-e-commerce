import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/app_state.dart';
import '../models/product.dart';
import '../pages/product_detail_page.dart';
import 'common.dart';

/// 价格组件：闪购价红色 + 划线原价 + 折扣角标。
class PriceTag extends StatelessWidget {
  final ProductCard product;
  final double size;

  const PriceTag({super.key, required this.product, this.size = 16});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final flash = product.hasFlash;
    final main = product.effectivePrice;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        if (main != null)
          Text(
            main.display,
            style: TextStyle(
              fontSize: size,
              fontWeight: FontWeight.bold,
              color: flash ? theme.colorScheme.error : theme.colorScheme.onSurface,
            ),
          ),
        if (flash && product.price != null) ...[
          const SizedBox(width: 6),
          Text(
            product.price!.display,
            style: TextStyle(
              fontSize: size - 5,
              color: Colors.grey,
              decoration: TextDecoration.lineThrough,
            ),
          ),
        ],
      ],
    );
  }
}

/// 商品卡（瀑布流/网格通用）。
class ProductCardView extends StatelessWidget {
  final ProductCard product;
  const ProductCardView({super.key, required this.product});

  @override
  Widget build(BuildContext context) {
    final app = context.read<AppState>();
    return InkWell(
      onTap: () {
        Navigator.of(context).push(
          MaterialPageRoute(builder: (_) => ProductDetailPage(productId: product.id)),
        );
      },
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AspectRatio(
            aspectRatio: 1,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Stack(
                fit: StackFit.expand,
                children: [
                  NetImage(product.mainImage),
                  if (product.discountPercent != null)
                    Positioned(
                      left: 0,
                      top: 0,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.error,
                          borderRadius: const BorderRadius.only(bottomRight: Radius.circular(8)),
                        ),
                        child: Text(
                          '-${product.discountPercent}%',
                          style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 6),
          Text(
            product.name,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 13, height: 1.25),
          ),
          const SizedBox(height: 4),
          PriceTag(product: product),
          const SizedBox(height: 2),
          Row(
            children: [
              const Icon(Icons.star, size: 12, color: Colors.amber),
              const SizedBox(width: 2),
              Text(product.ratingAvg.toStringAsFixed(1),
                  style: TextStyle(fontSize: 11, color: Colors.grey.shade600)),
              const SizedBox(width: 8),
              Text('${product.salesCount} ${app.t('sold')}',
                  style: TextStyle(fontSize: 11, color: Colors.grey.shade500)),
            ],
          ),
        ],
      ),
    );
  }
}
