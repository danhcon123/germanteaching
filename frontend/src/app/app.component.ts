import {Component} from '@angular/core';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})

export class AppComponent {
    title = 'Hello, Angular World! 🎉';
    ngOnInit() {
        console.log('🚀 AppComponent initialized, title =', this.title);
    }
}